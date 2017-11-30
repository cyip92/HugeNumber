# What
Java library capable of representing unreasonably large numbers via a recursive floating point-style representation and a bit of cleverness with discarding numbers that don't matter any more.  Probably capable of going up to 10 ↑↑ 100000000 or so (Knuth up-arrow notation).  This necessarily results in a loss of precision for extremely large numbers, use at your own risk!

Currently only guaranteed to work well with large positive numbers and is in general very much a work in progress.  Anything above 10 ↑↑ 1000000 remains untested for now as there isn't a good way right now to make any numbers much bigger.

# How

Internally stores number as (*mantissa*)\*10^(*exponent*), where *exponent* is itself a HugeNumber, recursively until *exponent* is less than 10.  Past a certain recursion depth it collapses the whole stack down and just keeps track of the number of recursion layers and the final exponent, and much farther past that it only tracks the recursion depth.

Whenever some parts of the number are deemed irrelevent due to the necessary loss of precision in other floating point numbers, the displayed number is instead randomized based on the Java hashCode() function.  This is meant to produce nice-looking numbers which also don't change if the number itself isn't changed.

# Why

Because I wanted a way to do math with numbers that can't possibly fit in the universe when written down.
