## Bonus marks

I added a formatter to the build.xml file which will display failing test cases
nicely at the end of standard output, if you would like to [check that here](https://travis-ci.org/frdevilliers/green/builds/410216347)

[![Build Status](https://travis-ci.org/frdevilliers/green.svg?branch=master)](https://travis-ci.org/frdevilliers/green?branch=master)

Notes:

The first step is to update "build.properties" with your local
settings.  You do not need to set z3 and latte, but in that case
some unit tests won't run.
   
