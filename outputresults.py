import xml.etree.ElementTree as ET
import sys

tree = ET.parse('./bin/junit/TEST-za.ac.sun.cs.green.EntireSuite.xml')
root = tree.getroot()
hadfail = False

for test in root.findall("./testcase"):
    if test.findall("failure"):
        hadfail = True
        print test.attrib['name'], " Failed", test.find("failure").attrib['message']
    else:
        print test.attrib['name'], "Passed"

if hadfail == True:
    sys.exit(1)
else:
    sys.exit(0)
