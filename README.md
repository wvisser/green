[![Build Status](https://travis-ci.org/craigils/green.svg?branch=master)](https://travis-ci.org/craigils/green)

### Deliverable 1
___

I realised that the ```check()``` function takes 3 parameters
1. An Operation which is constructed (using the programs functions) as a part of each test case.
2. A String of what the Operation is expected to look like.
3. The canonized (most simplified) form of the expression.

To fix the test, I followed the code into the ```finalCheck()``` method and printed the inputted vs expected canonized forms to find where the test was wrong.

I then changed the expectation (the third parameter) to match that.

The correct 3rd parameter for the check in test20 was ```1*v+-1<=0```.
