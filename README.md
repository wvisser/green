## Bonus marks

I added a formatter to the build.xml file which will display failing test cases
nicely at the end of standard output, if you would like to [check that here](https://github.com/frdevilliers/green/commit/b90f277eda2c12fa3a7ca5c53acbf41e74c4e8f2).

[![Build Status](https://travis-ci.org/frdevilliers/green.svg?branch=master)](https://travis-ci.org/frdevilliers/green?branch=master)

Notes:

The first step is to update "build.properties" with your local
settings.  You do not need to set z3 and latte, but in that case
some unit tests won't run.
   
