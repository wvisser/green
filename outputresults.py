import xml.etree.ElementTree as ET
import sys

with open('./bin/junit/TEST-za.ac.sun.cs.green.EntireSuite.xml', 'r') as fin:
    print fin.read()

tree = ET.parse('./bin/junit/TEST-za.ac.sun.cs.green.EntireSuite.xml')
root = tree.getroot()
hadfail = False

for test in root.findall("./testcase"):
    if test.findall("failure"):
        hadfail = True
        print "Failed ", test
        # print "Test", test.attrib['name'], " Failed", test.find("failure").attrib['message']
    else:
        print "Passed ", test

if hadfail == True:
    sys.exit(1)
else:
    sys.exit(0)
