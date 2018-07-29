import xml.etree.ElementTree as ET
tree = ET.parse('./bin/junit/TESTS-TestSuites.xml')
root = tree.getroot()

for test in root.findall("./testsuite/testcase"):
    if test.findall("failure"):
        print testcase.attrib['name'], " Failed", testcase.findall("failure")[0].attrib['message']
    else:
        print testcase.attrib['name'], "Passed"
