package com.ncert7.mathandsciencelab.ui.screens.conceptscreen.components

import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.ncert7.mathandsciencelab.service.analytics.InteractionTracker

/** WebView component for rendering simulation HTML */
@Composable
fun SimulationWebView(
    url: String,
    modifier: Modifier = Modifier,
    onPageLoaded: () -> Unit = {}
) {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    loadWithOverviewMode = true
                    useWideViewPort = true
                }

                addJavascriptInterface(object {
                    @JavascriptInterface
                    fun logButtonClick(buttonName: String) {
                        InteractionTracker.logInteraction(buttonName)
                    }

                    @JavascriptInterface
                    fun logVerdict(isCorrect: Boolean) {
                        InteractionTracker.logVerdict(isCorrect)
                    }
                }, "AndroidBridge")

                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)

                        val script = """
                            (function() {

                                function sendEvent(name) {
                                    if (window.AndroidBridge && name && name.trim().length > 0) {
                                        window.AndroidBridge.logButtonClick(name.trim());
                                    }
                                }

                                function isClickable(el) {
                                    try {
                                        return window.getComputedStyle(el).cursor === 'pointer';
                                    } catch(e) {
                                        return false;
                                    }
                                }

                                // ── 1. Click tracking ─────────────────────────────────────────
                                // Hold buttons (GAS, BRAKE) fire mousedown/touchstart not click.
                                // Normal taps fire mousedown + touchstart + click almost at once.
                                // Single shared cooldown: only the first event within 500ms logs.

                                var lastLoggedText = '';
                                var lastLoggedTime = 0;

                                function sendButtonEvent(text) {
                                    var now = Date.now();
                                    if (text === lastLoggedText && (now - lastLoggedTime) < 500) return;
                                    lastLoggedText = text;
                                    lastLoggedTime = now;
                                    sendEvent(text);
                                }

                                function getButtonText(el) {
                                    var btn = el.closest('button, [role="button"], a');
                                    if (!btn) return '';
                                    return (btn.innerText || btn.getAttribute('aria-label') || btn.title || '').trim();
                                }

                                // mousedown — catches hold buttons like GAS, BRAKE
                                document.addEventListener('mousedown', function(event) {
                                    var text = getButtonText(event.target);
                                    if (text.length > 0) sendButtonEvent(text);
                                }, true);

                                // touchstart — catches hold buttons on touch devices
                                document.addEventListener('touchstart', function(event) {
                                    var text = getButtonText(event.target);
                                    if (text.length > 0) sendButtonEvent(text);
                                }, true);

                                // click — button-like elements + pointer-cursor leaf nodes
                                document.addEventListener('click', function(event) {
                                    var el = event.target;

                                    // (a) Button-like — deduplicated via sendButtonEvent
                                    var btn = el.closest('button, [role="button"], a');
                                    if (btn) {
                                        var text = (btn.innerText || btn.getAttribute('aria-label') || btn.title || '').trim();
                                        if (text.length > 0) sendButtonEvent(text);
                                        return;
                                    }

                                    // (b) Pointer-cursor leaf nodes — interactive answer cards
                                    var candidate = el;
                                    var found = false;
                                    for (var i = 0; i < 3; i++) {
                                        if (!candidate || candidate === document.body) break;
                                        if (isClickable(candidate) && candidate.children.length <= 1) {
                                            found = true;
                                            break;
                                        }
                                        candidate = candidate.parentElement;
                                    }
                                    if (found) {
                                        var text = (candidate.innerText || '').trim();
                                        if (text.length > 0 && text.length <= 60) {
                                            sendEvent(text);
                                        }
                                    }
                                }, true);

                                // ── 2. Slider and input tracking ──────────────────────────────

                                document.addEventListener('change', function(event) {
                                    var el = event.target;
                                    if (el.tagName === 'INPUT') {
                                        var label = el.placeholder || el.getAttribute('aria-label') || el.name || el.id || 'Input';
                                        if (el.type === 'range') {
                                            sendEvent("Slider [" + label + "] set to: " + el.value);
                                        } else {
                                            sendEvent("Entered [" + label + "]: " + el.value);
                                        }
                                    }
                                }, true);

                                // ── 3. Verdict detection ──────────────────────────────────────

                                var WRONG_PHRASE_RE = /\b(not correct|not yet|not quite|try again|not right|check again|Current sides|look again|that's not|wrong answer|needs work|need to|keep trying|lowest form|correct count|correct answer is|correct fix|not target|not reached|Chain incomplete|Target was|Correct form|Not exact|Closest is|Unsafe choice|Off target|Target is|Correct value)\b/i;
                                var WRONG_WORD_RE   = /\b(wrong|incorrect|Expected|oops|mismatch|mistake|error|nope|revisit|rethink|need)\b/i;

                                var CORRECT_PHRASE_RE = /\b(well done|great job|right answer|you got it|that's correct|good job|spot on|nicely done|well played|correct classification|Proof locked|Target built|Safe dispatch chosen|Grid unlocked)\b/i;
                                var CORRECT_WORD_RE   = /\b(correct|correctly|great|success|excellent|bullseye|simplest|perfect|perfectly|bravo|amazing|awesome|fantastic|superb|brilliant|nailed|congrats|congratulations|solved|yay|well|done|achieved)\b/i;

                                function isWrong(text) {
                                    return WRONG_PHRASE_RE.test(text) || WRONG_WORD_RE.test(text);
                                }

                                function isCorrectVerdict(text) {
                                    return CORRECT_PHRASE_RE.test(text) || CORRECT_WORD_RE.test(text);
                                }

                                function isGreenish(rgb) {
                                    var m = rgb.match(/\d+/g);
                                    if (!m || m.length < 3) return false;
                                    var r = +m[0], g = +m[1], b = +m[2];
                                    return g > 80 && g > r * 1.3 && g > b * 1.2;
                                }

                                function isReddish(rgb) {
                                    var m = rgb.match(/\d+/g);
                                    if (!m || m.length < 3) return false;
                                    var r = +m[0], g = +m[1], b = +m[2];
                                    return r > 80 && r > g * 1.3 && r > b * 1.3;
                                }

                                var verdictTimer = null;

                                function scheduleVerdict(isCorrectResult) {
                                    if (verdictTimer !== null) clearTimeout(verdictTimer);
                                    verdictTimer = setTimeout(function() {
                                        verdictTimer = null;
                                        if (window.AndroidBridge) {
                                            window.AndroidBridge.logVerdict(isCorrectResult);
                                        }
                                    }, 300);
                                }

                                function evaluateElement(el) {
                                    if (el.nodeType !== 1) return false;
                                    var text = el.innerText || el.textContent || '';
                                    if (text.length === 0) return false;

                                    var bg = '';
                                    try { bg = window.getComputedStyle(el).backgroundColor || ''; } catch(e) {}

                                    var green = isGreenish(bg);
                                    var red   = isReddish(bg);

                                    if (red) {
                                        if (isWrong(text)) { scheduleVerdict(false); return true; }
                                        return false;
                                    }
                                    if (green) {
                                        if (isCorrectVerdict(text)) { scheduleVerdict(true); return true; }
                                        return false;
                                    }

                                    if (isWrong(text))          { scheduleVerdict(false); return true; }
                                    if (isCorrectVerdict(text)) { scheduleVerdict(true);  return true; }
                                    return false;
                                }

                                function isVisible(el) {
                                    try {
                                        var style = window.getComputedStyle(el);
                                        return style.display !== 'none' &&
                                               style.visibility !== 'hidden' &&
                                               style.opacity !== '0';
                                    } catch(e) {
                                        return false;
                                    }
                                }

                                function checkNewNode(node) {
                                    if (node.nodeType !== 1) return;
                                    if (!isVisible(node)) return;
                                    setTimeout(function() {
                                        if (evaluateElement(node)) return;
                                        var children = node.children;
                                        for (var k = 0; k < children.length; k++) {
                                            if (evaluateElement(children[k])) return;
                                        }
                                    }, 0);
                                }

                                var observer = new MutationObserver(function(mutations) {
                                    for (var i = 0; i < mutations.length; i++) {
                                        var mutation = mutations[i];
                                        if (mutation.type === 'childList') {
                                            var added = mutation.addedNodes;
                                            for (var j = 0; j < added.length; j++) {
                                                checkNewNode(added[j]);
                                            }
                                        }
                                        if (mutation.type === 'attributes') {
                                            checkNewNode(mutation.target);
                                        }
                                    }
                                });

                                observer.observe(document.body, {
                                    childList: true,
                                    subtree: true,
                                    attributes: true,
                                    attributeFilter: ['style', 'class']
                                });

                            })();
                        """.trimIndent()
                        view?.evaluateJavascript(script, null)
                        onPageLoaded()
                    }
                }
                loadUrl(url)
            }
        },
        modifier = modifier.fillMaxSize()
    )
}