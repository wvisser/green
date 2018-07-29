import xml.etree.ElementTree as ET
import sys

tree = ET.parse('./bin/junit/TEST-za.ac.sun.cs.green.EntireSuite.xml')
root = tree.getroot()
hadfail = false;

for test in root.findall("./testcase"):
    if test.findall("failure"):
        hadfail = true
        if test.attrib['classname'] == "za.ac.sun.cs.green.service.canonizer.SATCanonizerTest":
            print "SATCanonizerTest: ", test.attrib['name'], " Failed", test.find("failure").attrib['message']
        else:
            print "Z3Test: ", test.attrib['name'], " Failed", test.find("failure").attrib['message']
    else:
        if test.attrib['classname'] == "za.ac.sun.cs.green.service.canonizer.SATCanonizerTest":
            print "SATCanonizerTest: ", test.attrib['name'], "Passed"
        else:
            print "Z3Test: ", test.attrib['name'], "Passed"

if hadfail == true:
    return sys.exit(1)
else:
    return sys.exit(0)
