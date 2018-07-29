import xml.etree.ElementTree as ET
tree = ET.parse('./bin/junit/TEST-za.ac.sun.cs.green.EntireSuite.xml')
root = tree.getroot()

for test in root.findall("./testsuites/testsuite/testcase"):
    if test.findall("failure"):
        if test.attrib['classname'] == "za.ac.sun.cs.green.service.canonizer.SATCanonizerTest":
            print "SATCanonizerTest: ", test.attrib['name'], " Failed", test.find("failure").attrib['message']
        else:
            print "Z3Test: ", test.attrib['name'], " Failed", test.find("failure").attrib['message']
    else:
        if test.attrib['classname'] == "za.ac.sun.cs.green.service.canonizer.SATCanonizerTest":
            print "SATCanonizerTest: ", test.attrib['name'], "Passed"
        else:
            print "Z3Test: ", test.attrib['name'], "Passed"
