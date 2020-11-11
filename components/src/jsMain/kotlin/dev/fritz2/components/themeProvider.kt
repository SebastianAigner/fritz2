package dev.fritz2.components

import DefaultTheme
import dev.fritz2.binding.RootStore
import dev.fritz2.binding.SimpleHandler
import dev.fritz2.dom.Listener
import dev.fritz2.dom.html.Div
import dev.fritz2.dom.html.HtmlElements
import dev.fritz2.dom.html.render
import dev.fritz2.styling.StyleClass
import dev.fritz2.styling.params.FlexParams
import dev.fritz2.styling.resetCss
import dev.fritz2.styling.theme.Theme
import dev.fritz2.styling.theme.currentTheme
import dev.fritz2.styling.theme.theme
import kotlinx.coroutines.flow.Flow

interface ThemeStore {
    val data: Flow<Int>
    val selectTheme: SimpleHandler<Int>
}


/**
 * This class offers the configuration of the [themeProvider] component.
 *
 * The component offers some configurable features:
 * - to set one or a [list][List] of themes; if given a list, the first theme of the list will be taken as current theme
 *   automatically.
 * - to enable or disable the resetting of the browser's default styling (it is highly recommended to stick with the
 *   default of resetting!) The reset procedure uses theme specific values already, so the basic look and feel of the app
 *   will comply to the theme.
 * - to pass in arbitrary content of course, as this component acts as the root of all UI
 * - it offers access to the [themeStore] in order to enable the _dynamic_ switching between different [themes][Theme]
 *   at runtime.
 *
 * The pattern to integrate a [themeProvider] into an app resembles always the following examples:
 * ```
 * // minimal integration: Stick to the default theme and reset the browser's CSS
 * render { theme: Theme -> // gain access to the specific (sub-)*type* of your theme and the initial theme
 *     themeProvider { // configure the provider itself -> nothing theme specific here, so the [DefaultTheme] will be used
 *          items {
 *              // your UI goes here
 *          }
 *     }.mount("target")
 * ```
 *
 * Sometimes you want to set a theme that differs from the _default_ theme:
 * ```
 * render { theme: ExtendedTheme -> // gain access to the specific (sub-)*type* of your theme and the initial theme
 *     themeProvider {
 *          theme { myThemeInstance } // set the desired theme
 *          items {
 *              // your UI goes here
 *          }
 *     }.mount("target")
 * ```
 * If you want to enable active switching between two themes, you have to _grab_ the theme store in order to pass a
 * fitting flow (of a selection component probably) into it
 * ```
 * // prepare some collection of themes:
 * val themes = listOf<ExtendedTheme>(
 *      Light(),
 *      Dark()
 * )
 *
 * // set the themes!
 * render { theme: ExtendedTheme -> // gain access to the specific (sub-)*type* of your theme and the initial theme
 *     themeProvider {
 *          themes { themes } // set the desired themes
 *          items {
 *              // use the exposed ``themeStore`` to dynamically select the current theme
 *              themeStore.data.map { currentThemeIndex -> // grab the current index to deduce the name later on
 *                  radioGroup {
 *                      items { themes.map { it.name } } // provide a list of names
 *                      selected { themes[currentThemeIndex].name } // set the selected name
 *                  }.map { selected -> // derive the index of the selected theme via its name
 *                      themes.indexOf(
 *                          themes.find {
 *                              selected == it.name
 *                          }
 *                      )
 *                  } handledBy themeStore.selectTheme // use the exposed ``themeStore`` as handler
 *              }
 *          }.watch() // must be watched, as there is nothing bound!
 *     }.mount("target")
 * ```
 */
class ThemeComponent {
    companion object {
        val staticResetCss: String
            get() = """ 
/*! normalize.css v8.0.1 | MIT License | github.com/necolas/normalize.css */

/* Document
   ========================================================================== */

/**
 * 1. Correct the line height in all browsers.
 * 2. Prevent adjustments of font size after orientation changes in iOS.
 */

html {
  line-height: 1.15; /* 1 */
  -webkit-text-size-adjust: 100%; /* 2 */
}

/* Sections
   ========================================================================== */

/**
 * Remove the margin in all browsers.
 */

body {
  margin: 0;
}

/**
 * Render the `main` element consistently in IE.
 */

main {
  display: block;
}

/**
 * Correct the font size and margin on `h1` elements within `section` and
 * `article` contexts in Chrome, Firefox, and Safari.
 */

h1 {
  font-size: ${theme().fontSizes.huge};
  margin: 0.67em 0;
}

/* Grouping content
   ========================================================================== */

/**
 * 1. Add the correct box sizing in Firefox.
 * 2. Show the overflow in Edge and IE.
 */

hr {
  box-sizing: content-box; /* 1 */
  height: 0; /* 1 */
  overflow: visible; /* 2 */
}

/**
 * 1. Correct the inheritance and scaling of font size in all browsers.
 * 2. Correct the odd `em` font sizing in all browsers.
 */

pre {
  font-family: monospace, monospace; /* 1 */
  font-size: ${theme().fontSizes.normal}; /* 2 */
}

/* Text-level semantics
   ========================================================================== */

/**
 * Remove the gray background on active links in IE 10.
 */

a {
  background-color: transparent;
}

/**
 * 1. Remove the bottom border in Chrome 57-
 * 2. Add the correct text decoration in Chrome, Edge, IE, Opera, and Safari.
 */

abbr[title] {
  border-bottom: none; /* 1 */
  text-decoration: underline; /* 2 */
  -webkit-text-decoration: underline dotted;
          text-decoration: underline dotted; /* 2 */
}

/**
 * Add the correct font weight in Chrome, Edge, and Safari.
 */

b,
strong {
  font-weight: bolder;
}

/**
 * 1. Correct the inheritance and scaling of font size in all browsers.
 * 2. Correct the odd `em` font sizing in all browsers.
 */

code,
kbd,
samp {
  font-family: monospace, monospace; /* 1 */
  font-size: ${theme().fontSizes.huge};; /* 2 */
}

/**
 * Add the correct font size in all browsers.
 */

small {
  font-size: 80%;
}

/**
 * Prevent `sub` and `sup` elements from affecting the line height in
 * all browsers.
 */

sub,
sup {
  font-size: 75%;
  line-height: 0;
  position: relative;
  vertical-align: baseline;
}

sub {
  bottom: -0.25em;
}

sup {
  top: -0.5em;
}

/* Embedded content
   ========================================================================== */

/**
 * Remove the border on images inside links in IE 10.
 */

img {
  border-style: none;
}

/* Forms
   ========================================================================== */

/**
 * 1. Change the font styles in all browsers.
 * 2. Remove the margin in Firefox and Safari.
 */

button,
input,
optgroup,
select,
textarea {
  font-family: inherit; /* 1 */
  font-size: ${theme().fontSizes.normal};; /* 1 */
  line-height: 1.15; /* 1 */
  margin: 0; /* 2 */
}

/**
 * Show the overflow in IE.
 * 1. Show the overflow in Edge.
 */

button,
input { /* 1 */
  overflow: visible;
}

/**
 * Remove the inheritance of text transform in Edge, Firefox, and IE.
 * 1. Remove the inheritance of text transform in Firefox.
 */

button,
select { /* 1 */
  text-transform: none;
}

/**
 * Correct the inability to style clickable types in iOS and Safari.
 */

button,
[type="button"],
[type="reset"],
[type="submit"] {
  -webkit-appearance: button;
}

/**
 * Remove the inner border and padding in Firefox.
 */

button::-moz-focus-inner,
[type="button"]::-moz-focus-inner,
[type="reset"]::-moz-focus-inner,
[type="submit"]::-moz-focus-inner {
  border-style: none;
  padding: 0;
}

/**
 * Restore the focus styles unset by the previous rule.
 */

button:-moz-focusring,
[type="button"]:-moz-focusring,
[type="reset"]:-moz-focusring,
[type="submit"]:-moz-focusring {
  outline: 1px dotted ButtonText;
}

/**
 * Correct the padding in Firefox.
 */

fieldset {
  padding: 0.35em 0.75em 0.625em;
}

/**
 * 1. Correct the text wrapping in Edge and IE.
 * 2. Correct the color inheritance from `fieldset` elements in IE.
 * 3. Remove the padding so developers are not caught out when they zero out
 *    `fieldset` elements in all browsers.
 */

legend {
  box-sizing: border-box; /* 1 */
  color: inherit; /* 2 */
  display: table; /* 1 */
  max-width: 100%; /* 1 */
  padding: 0; /* 3 */
  white-space: normal; /* 1 */
}

/**
 * Add the correct vertical alignment in Chrome, Firefox, and Opera.
 */

progress {
  vertical-align: baseline;
}

/**
 * Remove the default vertical scrollbar in IE 10+.
 */

textarea {
  overflow: auto;
}

/**
 * 1. Add the correct box sizing in IE 10.
 * 2. Remove the padding in IE 10.
 */

[type="checkbox"],
[type="radio"] {
  box-sizing: border-box; /* 1 */
  padding: 0; /* 2 */
}

/**
 * Correct the cursor style of increment and decrement buttons in Chrome.
 */

[type="number"]::-webkit-inner-spin-button,
[type="number"]::-webkit-outer-spin-button {
  height: auto;
}

/**
 * 1. Correct the odd appearance in Chrome and Safari.
 * 2. Correct the outline style in Safari.
 */

[type="search"] {
  -webkit-appearance: textfield; /* 1 */
  outline-offset: -2px; /* 2 */
}

/**
 * Remove the inner padding in Chrome and Safari on macOS.
 */

[type="search"]::-webkit-search-decoration {
  -webkit-appearance: none;
}

/**
 * 1. Correct the inability to style clickable types in iOS and Safari.
 * 2. Change font properties to `inherit` in Safari.
 */

::-webkit-file-upload-button {
  -webkit-appearance: button; /* 1 */
  font: inherit; /* 2 */
}

/* Interactive
   ========================================================================== */

/*
 * Add the correct display in Edge, IE 10+, and Firefox.
 */

details {
  display: block;
}

/*
 * Add the correct display in all browsers.
 */

summary {
  display: list-item;
}

/* Misc
   ========================================================================== */

/**
 * Add the correct display in IE 10+.
 */

template {
  display: none;
}

/**
 * Add the correct display in IE 10.
 */

[hidden] {
  display: none;
}

/**
 * Manually forked from SUIT CSS Base: https://github.com/suitcss/base
 * A thin layer on top of normalize.css that provides a starting point more
 * suitable for web applications.
 */

/**
 * 1. Prevent padding and border from affecting element width
 * https://goo.gl/pYtbK7
 * 2. Change the default font family in all browsers (opinionated)
 */

html {
  box-sizing: border-box; /* 1 */
  font-family: sans-serif; /* 2 */
}

*,
*::before,
*::after {
  box-sizing: inherit;
}

/**
 * Removes the default spacing and border for appropriate elements.
 */

blockquote,
dl,
dd,
h1,
h2,
h3,
h4,
h5,
h6,
hr,
figure,
p,
pre {
  margin: 0;
}

button {
  background: transparent;
  padding: 0;
}

/**
 * Work around a Firefox/IE bug where the transparent `button` background
 * results in a loss of the default `button` focus styles.
 */

button:focus {
  outline: 1px dotted;
  outline: 5px auto -webkit-focus-ring-color;
}

fieldset {
  margin: 0;
  padding: 0;
}

ol,
ul {
  list-style: none;
  margin: 0;
  padding: 0;
  padding-left: ${theme().space.normal};
}

ul {
  list-style-type: disc
}

ol {
  list-style-type: decimal
}

/**
 * Tailwind custom reset styles
 */

/**
 * 1. Use the system font stack as a sane default.
 * 2. Use Tailwind's default "normal" line-height so the user isn't forced
 * to override it to ensure consistency even when using the default theme.
 */

html {
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, "Noto Sans", sans-serif, "Apple Color Emoji", "Segoe UI Emoji", "Segoe UI Symbol", "Noto Color Emoji"; /* 1 */
  line-height: 1.5; /* 2 */
}

/**
 * Allow adding a border to an element by just adding a border-width.
 *
 * By default, the way the browser specifies that an element should have no
 * border is by setting it's border-style to `none` in the user-agent
 * stylesheet.
 *
 * In order to easily add borders to elements by just setting the `border-width`
 * property, we change the default border-style for all elements to `solid`, and
 * use border-width to hide them instead. This way our `border` utilities only
 * need to set the `border-width` property instead of the entire `border`
 * shorthand, making our border utilities much more straightforward to compose.
 *
 * https://github.com/tailwindcss/tailwindcss/pull/116
 */

*,
*::before,
*::after {
  border-width: 0;
  border-style: solid;
  border-color: #e2e8f0;
}

/*
 * Ensure horizontal rules are visible by default
 */

hr {
  border-top-width: 1px;
}

/**
 * Undo the `border-style: none` reset that Normalize applies to images so that
 * our `border-{width}` utilities have the expected effect.
 *
 * The Normalize reset is unnecessary for us since we default the border-width
 * to 0 on all elements.
 *
 * https://github.com/tailwindcss/tailwindcss/issues/362
 */

img {
  border-style: solid;
}

textarea {
  resize: vertical;
}

input:-ms-input-placeholder,
textarea:-ms-input-placeholder {
  color: #a0aec0;
}

input::-ms-input-placeholder,
textarea::-ms-input-placeholder {
  color: #a0aec0;
}

input::placeholder,
textarea::placeholder {
  color: #a0aec0;
}

button,
[role="button"] {
  cursor: pointer;
}

table {
  border-collapse: collapse;
}

h1 {
  font-size: ${theme().fontSizes.huge};;
  font-weight: bold;
}

h2 {
  font-size: ${theme().fontSizes.larger};;
  font-weight: bold;
}

h3 {
  font-size: ${theme().fontSizes.large};;
  font-weight: bold;
}

h4 {
  font-size: ${theme().fontSizes.normal};;
  font-weight: bold;
}

h5 {
  font-size: ${theme().fontSizes.small};;
  font-weight: bold;
}

h6 {
  font-size: ${theme().fontSizes.smaller};;
  font-weight: bold;
}

/**
 * Reset links to optimize for opt-in styling instead of
 * opt-out.
 */

a {
  color: inherit;
  text-decoration: inherit;
}

/**
 * Reset form element properties that are easy to forget to
 * style explicitly so you don't inadvertently introduce
 * styles that deviate from your design system. These styles
 * supplement a partial reset that is already applied by
 * normalize.css.
 */

button,
input,
optgroup,
select,
textarea {
  padding: 0;
  line-height: inherit;
  color: inherit;
}

/**
 * Use the configured 'mono' font family for elements that
 * are expected to be rendered with a monospace font, falling
 * back to the system monospace stack if there is no configured
 * 'mono' font family.
 */

pre,
code,
kbd,
samp {
  font-family: Menlo, Monaco, Consolas, "Liberation Mono", "Courier New", monospace;
}

/**
 * Make replaced elements `display: block` by default as that's
 * the behavior you want almost all of the time. Inspired by
 * CSS Remedy, with `svg` added as well.
 *
 * https://github.com/mozdevs/cssremedy/issues/14
 */

img,
svg,
video,
canvas,
audio,
iframe,
embed,
object {
  display: block;
  vertical-align: middle;
}

/**
 * Constrain images and videos to the parent width and preserve
 * their instrinsic aspect ratio.
 *
 * https://github.com/mozdevs/cssremedy/issues/14
 */

img,
video {
  max-width: 100%;
  height: auto;
}

/*# sourceMappingURL=base.css.map */                
            """.trimIndent()

    }

    var resetCss: Boolean = true

    fun resetCss(value: () -> Boolean) {
        resetCss = value()
    }

    var items: (HtmlElements.() -> Unit)? = null

    fun items(value: HtmlElements.() -> Unit) {
        items = value
    }

    var themes = listOf<Theme>(DefaultTheme())

    fun theme(value: () -> Theme) {
        themes = listOf(value())
    }

    fun themes(values: () -> List<Theme>) {
        themes = values()
    }

    // Expose ``ThemeStore`` via build-Block and bind it to a local val for further usage!
    val themeStore: ThemeStore = object : RootStore<Int>(0), ThemeStore {
        override val selectTheme = handle<Int> { _, index ->
            currentTheme = themes[index]
            index
        }
    }

    init {
        currentTheme = themes.first()
    }
}


/**
 * This component realizes an outer wrapper for the whole UI in order to set and initialize the actual theme
 * and to expose the [ThemeStore] in order to enable the _dynamic_ switching between different [themes][Theme]
 * at runtime.
 *
 * The component offers some configurable features:
 * - to set one or a [list][List] of themes
 * - to enable or disable the resetting of the browser's default styling (it is highly recommended to stick with the
 *   default of resetting!) The reset procedure uses theme specific values already, so the basic look and feel of the app
 *   will comply to the theme.
 * - to pass in arbitrary content of course, as this component acts as the root of all UI
 *
 * The pattern to integrate a [themeProvider] into an app resembles always the following examples:
 * ```
 * // minimal integration: Stick to the default theme and reset the browser's CSS
 * render { theme: ExtendedTheme -> // gain access to the specific (sub-)*type* of your theme and the initial theme
 *     themeProvider { // configure the provider itself -> nothing theme specific here, so the [DefaultTheme] will be used
 *          items {
 *              // your UI goes here
 *          }
 *     }.mount("target")
 * ```
 *
 * For a detailed overview of the configuration options have a look at [ThemeComponent]
 *
 * @see ThemeComponent
 *
 * @param styling a lambda expression for declaring the styling as fritz2's styling DSL
 * @param baseClass optional CSS class that should be applied to the element
 * @param id the ID of the element
 * @param prefix the prefix for the generated CSS class resulting in the form ``$prefix-$hash``
 * @param build a lambda expression for setting up the component itself. Details in [ThemeComponent]
 */
fun HtmlElements.themeProvider(
    styling: FlexParams.() -> Unit = {},
    baseClass: StyleClass? = null,
    id: String? = null,
    prefix: String = "box",
    build: ThemeComponent.() -> Unit = {}
): Div {
    val component = ThemeComponent().apply(build)

    return div {
        component.themeStore.data.render {
            if (component.resetCss) {
                resetCss(ThemeComponent.staticResetCss)
            }
            box(
                {
                    styling()
                    position {
                        fixed {
                            vertical { "0" }
                            horizontal { "0" }
                        }
                    }
                    overflow { auto }
                }, baseClass, id, prefix
            ) {
                component.items?.let { it() }
            }
        }.bind()
    }
}
