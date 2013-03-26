# utils

This folder contains various utilities for the development of the app.

## Code formatting

We aim to use the standard Android formatting rules - however we relax them a
little bit as far as JavaDoc is concerned.

Android provides two files that allow Eclipse to automatically format according
to the correct specs. See the following resources:

- A StackOverflow [question][1]
- the `android-formatting.xml` [file][2]
- the `android.importorder` [file][3]
- The [installation instructions][4]

Here are the installation instructions:

> You can import files in `development/ide/eclipse` to make Eclipse follow the
> Android style rules.
>
> 1. Select Window > Preferences > Java > Code Style.
> 2. Use Formatter > Import to import `android-formatting.xml`.
> 3. Organize Imports > Import to import `android.importorder`.

In addition to that, we also use the [eclipse-cs][5] checkstyle plugin. Once you
install it, use the file `checkstyle.xml` taken from [here][6] and slightly
adapted (removing some constraints). To do so, right-click on the project,
Properties > Checkstyle and choose *gist - shareme/948425* which should be
readily available.

[1]: http://stackoverflow.com/questions/2480596
[2]: https://raw.github.com/android/platform_development/master/ide/eclipse/android-formatting.xml
[3]: https://raw.github.com/android/platform_development/master/ide/eclipse/android.importorder
[4]: http://source.android.com/source/using-eclipse.html#eclipse-formatting
[5]: http://eclipse-cs.sourceforge.net/
[6]: https://gist.github.com/shareme/948425
