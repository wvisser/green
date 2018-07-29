import xml.etree.ElementTree as ET
tree = ET.parse('./bin/junit/TESTS-TestSuites.xml')
root = tree.getroot()

for test in root.findall("./testsuite/testcase"):
    if test.findall("failure"):
        print test.attrib['name'], " Failed", test.find("failure").attrib['message']
    else:
        print test.attrib['name'], "Passed"
