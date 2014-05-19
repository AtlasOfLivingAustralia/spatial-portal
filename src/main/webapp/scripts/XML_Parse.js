// Definition of each of the 12 node types:
// Node 1 = ELEMENT                   Node 2 = ATTRIBUTE
// Node 3 = TEXT                      Node 4 = CDATA SECTION
// Node 5 = ENTITY REFERENCE          Node 6 = ENTITY
// Node 7 = PROCESSING INSTRUCTION    Node 8 = COMMENT
// Node 9 = DOCUMENT                  Node 10= DOCUMENT TYPE
// Node 11= DOCUMENT FRAGMENT         Node 12= NOTATION

function showTree(objXMLDoc) {
    return  showChildNodes(objXMLDoc, 0);  // Show node info
}

function showChildNodes(objNode, intLevel) {
    var strNodes = '';                    // Accumulates description
    var intCount = 0;                     // No. of Child nodes
    var intNode = 0;                      // Index for child nodes

    if (objNode.nodeType != 3) {
        strNodes += '<BR>';
        for (intIndent = 0; intIndent < intLevel; intIndent++) {
            strNodes += '&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;';
        }
        strNodes += '<B> &nbsp;' + objNode.nodeName + '<B>';
        if (intLevel == 2) {
            strNodes += '----------------------------------------';
        }
    } else {
        strNodes += ': &nbsp; <B><FONT COLOR="#FF0000">' + objNode.nodeType + ' ' +
            objNode.nodeValue + '</FONT><B>';
    }

    if (objNode.childNodes) {
        if (objNode.childNodes.length > 0) {
            intCount = objNode.childNodes.length;
            for (intNode = 0; intNode < intCount; intNode++) {
                strNodes += showChildNodes(objNode.childNodes(intNode), intLevel + 1);
            }
        }
    }
    return strNodes;
}