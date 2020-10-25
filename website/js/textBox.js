//require bootstrap
//empties textbox on init
class TextBox{

    constructor(editorId, toolbarId, options){
        this.editor = document.getElementById(editorId);
        this.toolbar = document.getElementById(toolbarId);

        getCssFile("./css/textBox.css");

        //deal with options
        let toolbarArray;
        if("toolbar" in options){
            toolbarArray = options['toolbar'];
        }else{
            toolbarArray = ["size", "space", "bold", "italic", "underline", "space", "alignLeft", "alignMiddle", "alignRight"];
        }

        this.toolbar.classList.add("tB-toolbar");
        this.options = options;
        this.toolBtns = {};
        
        //toolbar buttons
        let len = toolbarArray.length;
        for(let i = 0; i < len; i++){
            let tool;
            //TODO: make event listeners of bold align to functions
            switch(toolbarArray[i]){
                case "alignLeft":
                    tool = document.createElement("button");
                    tool.classList.add("btn", "tB-toolButton", "tB-toolButton-active");
                    tool.innerHTML = "<i class=\"fas fa-align-left\"></i>";
                    tool.addEventListener("click", (event)=>{
                        if(tool.classList.contains("tB-toolButton-active")){
                            tool.classList.remove("tB-toolButton-active");
                        }else tool.classList.add("tB-toolButton-active");
                        this.setButtonInactive("alignMiddle")
                        this.setButtonInactive("alignRight")
                        this.setButtonActive("alignLeft")
                        document.execCommand('justifyleft');
                        if("on"+toolbarArray[i] in this.options){
                            let selVar = this.getSelectionPureTextIndex();
                            try{
                                this.options["on"+toolbarArray[i]]({"pIndex":selVar[0], "startIndex":selVar[1], "endIndex":selVar[2]})
                            }catch(err){
                                
                            }
                        }
                        this.editor.focus();
                    }, false);
                    break;
                case "alignMiddle":
                    tool = document.createElement("button");
                    tool.classList.add("btn", "tB-toolButton");
                    tool.innerHTML = "<i class=\"fas fa-align-center\"></i>";
                    tool.addEventListener("click", (event)=>{
                        if(tool.classList.contains("tB-toolButton-active")){
                            tool.classList.remove("tB-toolButton-active");
                        }else tool.classList.add("tB-toolButton-active");
                        this.setButtonActive("alignMiddle")
                        this.setButtonInactive("alignRight")
                        this.setButtonInactive("alignLeft")
                        document.execCommand('justifycenter');
                        if("on"+toolbarArray[i] in this.options){
                            let selVar = this.getSelectionPureTextIndex();
                            try{
                                this.options["on"+toolbarArray[i]]({"pIndex":selVar[0], "startIndex":selVar[1], "endIndex":selVar[2]})
                            }catch(err){
                                
                            }
                        }
                        this.editor.focus();
                    }, false);
                    break;
                case "alignRight":
                    tool = document.createElement("button");
                    tool.classList.add("btn", "tB-toolButton");
                    tool.innerHTML = "<i class=\"fas fa-align-right\"></i>";
                    tool.addEventListener("click", (event)=>{
                        if(tool.classList.contains("tB-toolButton-active")){
                            tool.classList.remove("tB-toolButton-active");
                        }else tool.classList.add("tB-toolButton-active");
                        this.setButtonInactive("alignMiddle")
                        this.setButtonActive("alignRight")
                        this.setButtonInactive("alignLeft")
                        document.execCommand('justifyright');
                        if("on"+toolbarArray[i] in this.options){
                            let selVar = this.getSelectionPureTextIndex();
                            try{
                                this.options["on"+toolbarArray[i]]({"pIndex":selVar[0], "startIndex":selVar[1], "endIndex":selVar[2]})
                            }catch(err){
                                
                            }
                        }
                        this.editor.focus();
                    }, false);
                    break;
                case "space":
                    tool = document.createElement("p");
                    tool.classList.add("tB-margin");
                    break;
                case "bold":
                    tool = document.createElement("button");
                    tool.classList.add("btn", "tB-toolButton");
                    tool.innerHTML = "<i class=\"fas fa-bold\"></i>";
                    tool.addEventListener("click", (event)=>{
                        if(tool.classList.contains("tB-toolButton-active")){
                            tool.classList.remove("tB-toolButton-active");
                        }else{
                            tool.classList.add("tB-toolButton-active");
                        }
                        document.execCommand('bold');
                        if("on"+toolbarArray[i] in this.options){
                            let selVar = this.getSelectionPureTextIndex();
                            try{
                                this.options["on"+toolbarArray[i]]({"pIndex":selVar[0], "startIndex":selVar[1], "endIndex":selVar[2]})
                            }catch(err){
                                
                            }
                        }
                        this.editor.focus();
                    }, false);
                    break;
                case "italic":
                    tool = document.createElement("button");
                    tool.classList.add("btn", "tB-toolButton");
                    tool.innerHTML = "<i class=\"fas fa-italic\"></i>";
                    tool.addEventListener("click", (event)=>{
                        if(tool.classList.contains("tB-toolButton-active")){
                            tool.classList.remove("tB-toolButton-active");
                        }else tool.classList.add("tB-toolButton-active");
                        document.execCommand('italic');
                        if("on"+toolbarArray[i] in this.options){
                            let selVar = this.getSelectionPureTextIndex();
                            try{
                                this.options["on"+toolbarArray[i]]({"pIndex":selVar[0], "startIndex":selVar[1], "endIndex":selVar[2]})
                            }catch(err){
                                
                            }
                        }
                        this.editor.focus();
                    }, false);
                    break;
                case "underline":
                    tool = document.createElement("button");
                    tool.classList.add("btn", "tB-toolButton");
                    tool.innerHTML = "<i class=\"fas fa-underline\"></i>";
                    tool.addEventListener("click", (event)=>{
                        if(tool.classList.contains("tB-toolButton-active")){
                            tool.classList.remove("tB-toolButton-active");
                        }else tool.classList.add("tB-toolButton-active");
                        document.execCommand('underline');
                        if("on"+toolbarArray[i] in this.options){
                            let selVar = this.getSelectionPureTextIndex();
                            try{
                                this.options["on"+toolbarArray[i]]({"pIndex":selVar[0], "startIndex":selVar[1], "endIndex":selVar[2]})
                            }catch(err){
                                
                            }
                        }
                        this.editor.focus();
                    }, false);
                    break;
                case "size":
                    tool = document.createElement("div");
                    tool.classList.add("tB-tool-divWrap");

                    let btn = document.createElement("button");
                    btn.classList.add("btn", "tB-toolButton", "tB-toolButton-long");
                    btn.value = "normal";
                    btn.id = "tB-sizeSelect";
                    btn.innerHTML = "Size: Normal &nbsp;&nbsp;<span style='color:white'>|</span>&nbsp; <i class=\"fas fa-caret-up\"></i>";
                    tool.appendChild(btn);
                    btn.addEventListener("blur", (event) => {
                        this.editor.focus();
                    })

                    let dropdown = document.createElement("div");
                    dropdown.classList.add("tB-dropdown");
                    let buttonBig = document.createElement("button");
                    buttonBig.classList.add("btn", "tB-toolButton", "tB-toolButton-long", "tB-dropdownButton");
                    buttonBig.style.fontSize = "18px";
                    buttonBig.innerHTML = "Size: Big";
                    buttonBig.addEventListener("mouseover", (event) => {
                        btn.innerHTML = "Size: Big &nbsp;&nbsp;<span style='color:white'>|</span>&nbsp; <i class=\"fas fa-caret-up\"></i>";
                        btn.value = "big";
                        document.execCommand('fontsize', false, '5');
                        if("on"+toolbarArray[i] in this.options){
                            let selVar = this.getSelectionPureTextIndex();
                            try{
                                this.options["on"+toolbarArray[i]]({"pIndex":selVar[0], "startIndex":selVar[1], "endIndex":selVar[2], "size": '5'})
                            }catch(err){
                                
                            }
                        }
                        btn.focus();
                    })
                    dropdown.appendChild(buttonBig);
                    let buttonNormal = document.createElement("button");
                    buttonNormal.classList.add("btn", "tB-toolButton", "tB-toolButton-long", "tB-dropdownButton");
                    buttonNormal.style.fontSize = "16px";
                    buttonNormal.innerHTML = "Size: Normal";
                    buttonNormal.addEventListener("mouseover", (event) => {
                        btn.innerHTML = "Size: Normal &nbsp;&nbsp;<span style='color:white'>|</span>&nbsp; <i class=\"fas fa-caret-up\"></i>";
                        btn.value = "normal";
                        document.execCommand('fontsize', false, '3');
                        if("on"+toolbarArray[i] in this.options){
                            let selVar = this.getSelectionPureTextIndex();
                            try{
                                this.options["on"+toolbarArray[i]]({"pIndex":selVar[0], "startIndex":selVar[1], "endIndex":selVar[2], "size": '3'})
                            }catch(err){
                                
                            }
                        }
                        btn.focus();
                    })
                    dropdown.appendChild(buttonNormal);
                    let buttonSmall = document.createElement("button");
                    buttonSmall.classList.add("btn", "tB-toolButton", "tB-toolButton-long", "tB-dropdownButton");
                    buttonSmall.style.fontSize = "12px";
                    buttonSmall.innerHTML = "Size: Small";
                    buttonSmall.addEventListener("mouseover", (event) => {
                        btn.innerHTML = "Size: Small &nbsp;&nbsp;<span style='color:white'>|</span>&nbsp; <i class=\"fas fa-caret-up\"></i>";
                        btn.value = "small";
                        document.execCommand('fontsize', false, '2');
                        if("on"+toolbarArray[i] in this.options){
                            let selVar = this.getSelectionPureTextIndex();
                            try{
                                this.options["on"+toolbarArray[i]]({"pIndex":selVar[0], "startIndex":selVar[1], "endIndex":selVar[2], "size": '2'})
                            }catch(err){
                                
                            }
                        }
                        btn.focus();
                    })
                    dropdown.appendChild(buttonSmall);


                    
                    tool.appendChild(dropdown);

                    break;
            }
            if(toolbarArray[i] in this.toolBtns) this.toolBtns[toolbarArray[i]].push([tool])
            else this.toolBtns[toolbarArray[i]] = [tool]
            this.toolbar.appendChild(tool);
        }


        //Init editor
        this.editor.classList.add("tB-editor");
        this.editor.innerHTML = "";
        let firstParagraph = document.createElement("p");
        firstParagraph.style.textAlign = "left";
        this.editor.appendChild(firstParagraph);
        
        //init variables
        this.htmlContent = [""]; //in innerHTML
        this.elementContent = [document.createElement("p")];

        //options
        //newline function
        //console.log(options)
        if("onNewline" in options){
            this.onNewline = options['onNewline'];
        }else this.onNewline = this.empty;
        //onchange => {type: "", pIndex: int, index: "", index: int}
        if("onChange" in options){
            this.onChange = options['onChange'];
        }else this.onChange = this.empty;
        //onchange => {type: "", pIndex: int, index: "", index: int}
        if("onInput" in options){
            this.onInput = options['onInput'];
        }else this.onInput = this.empty;

        this.onNewWord = "onNewWord" in options ? options.onNewWord : this.empty;
        this.onCtrlEnter = "onCtrlEnter" in options ? options.onCtrlEnter : this.empty;

        this.editor.setAttribute("contentEditable", "true");
        
        this.editor.addEventListener("input", (event) => {
            //console.log(event);
            //console.log(event.inputType);
            if(event.inputType == "insertText"){
                if(event.data){
                    event.target.focus()

                    let {pIndex, selFromStart} = this.getSelectionPos();
                    this.insertTextLocal(pIndex, selFromStart.length - event.data.length, event.data);
                    //minus one because input event includes the change of the public one and it does not exist in the local



                }
            }else if(event.inputType == "insertCompositionText"){
                if(event.data.replace(/[a-zA-Z']/g, "").length > 0){
                    let {pIndex, selFromStart} = this.getSelectionPos();
                    this.insertTextLocal(pIndex, selFromStart.length - event.data.length, event.data);
                }
            }
            this.onInput();
        })
        this.editor.addEventListener("keyup", (event) => {
            //console.log(event);
            event.preventDefault();
        })
        this.editor.addEventListener("keydown", (event) => {
            //before changes to the text area
            if(event.code){
                if(event.ctrlKey && event.key == "Enter"){
                    //handle keyboard shortcut first
                    this.onCtrlEnter();
                }else if(event.key == "ArrowRight" || event.key == "ArrowLeft" || event.key == "ArrowUp" || event.key == "ArrowDown"){

                }else if(event.code.includes("Key") || event.code.includes("Digit")){
                    if(document.getSelection().getRangeAt(0).toString().length > 0){
                        let selection = this.getSelectionNative();
                        this.deleteSectionLocal(selection[0], selection[1], selection[2]);
                    }
                }else if(event.key == "Tab"){
                    if(document.getSelection().getRangeAt(0).toString().length > 0){
                        let selection = this.getSelectionNative();
                        this.deleteSectionLocal(selection[0], selection[1], selection[2]);
                    }
                    event.preventDefault();
                }else if(event.key == "Enter"){
                    //let newParagraph = document.createElement("p");
                    //newParagraph.id = "2";
                    //this.editor.appendChild(newParagraph);
                    //event.preventDefault();
                    this.onNewWord();

                    let selection = this.getSelectionNative();
                    this.elementContent.splice(selection[0]+1, 0, document.createElement("p"))
                    this.onChange("insertParagraph", )
                }else if(event.key === ' ' || event.key === 'Spacebar'){
                    this.onNewWord();
                }else if(event.key == "Backspace"){
                    if(this.editor.children.length == 1){
                        if(this.editor.children[0].innerText == "\n"){
                            this.editor.innerHTML = '<p style="text-align: left;"><br></p>'
                            this.editor.focus();
                            event.preventDefault();
                        }
                    }
                    
                    let selection = this.getSelectionNative();
                    if(selection[1] == selection[2]) selection[1] -= 1
                    this.deleteSectionLocal(selection[0], selection[1], selection[2]);
                }
            }
            
            //event.preventDefault();
        })
        document.addEventListener("selectionchange", (event) => {
            if(event.target.activeElement == this.editor){
                let bold = false;
                let italic = false;
                let underline = false;
                let node = document.getSelection().baseNode.parentNode;
                while(node.nodeName != "P"){
                    if(node.nodeName === "B") bold = true;
                    else if(node.nodeName === "I") italic = true;
                    else if(node.nodeName === "U") underline = true;
                    if(node.parentNode != null) node = node.parentNode
                    else break;
                }
                let paragraphNode = node
                node = document.getSelection().extentNode.parentNode;
                while(node.nodeName != "P"){
                    if(node.nodeName === "B") bold = true;
                    else if(node.nodeName === "I") italic = true;
                    else if(node.nodeName === "U") underline = true;
                    if(node.parentNode != null) node = node.parentNode
                    else break;
                }
                if(paragraphNode.style){
                    switch(paragraphNode.style.textAlign){
                        case "center":
                            this.setButtonActive("alignMiddle")
                            this.setButtonInactive("alignRight")
                            this.setButtonInactive("alignLeft")
                            break;
                        case "left":
                            this.setButtonInactive("alignMiddle")
                            this.setButtonInactive("alignRight")
                            this.setButtonActive("alignLeft")
                            break;
                        case "right":
                            this.setButtonInactive("alignMiddle")
                            this.setButtonActive("alignRight")
                            this.setButtonInactive("alignLeft")
                            break;
                        default:
                            this.setButtonInactive("alignMiddle")
                            this.setButtonInactive("alignRight")
                            this.setButtonInactive("alignLeft")
                            break;
                    }
                }
                if (bold){
                    this.setButtonActive("bold");
                }else this.setButtonInactive("bold");
                if (italic){
                    this.setButtonActive("italic");
                }else this.setButtonInactive("italic");
                if (underline){
                    this.setButtonActive("underline");
                }else this.setButtonInactive("underline");
            }
        })
        this.editor.addEventListener("cut", (event) => {
            
        })
        this.editor.addEventListener("copy", (e) => {
            e = e.originalEvent;
            var selectedText = window.getSelection();
            //console.log("original copied text\n--------\n", selectedText.toString());
            var range = selectedText.getRangeAt(0);
            var selectedTextReplacement = range.toString()
            //console.log("replacement in clipboard\n--------\n", selectedTextReplacement);
            e.clipboardData.setData('text/plain', selectedTextReplacement);
            e.preventDefault(); // default behaviour is to copy any selected text
        })
        this.editor.addEventListener("paste", (e) => {
            e.preventDefault();
            let content;
        
            if (window.clipboardData) {
                content = window.clipboardData.getData('Text');        
                if (window.getSelection) {
                    var selObj = window.getSelection();
                    var selRange = selObj.getRangeAt(0);
                    selRange.deleteContents();                
                    selRange.insertNode(document.createTextNode(content));
                }
            } else if (e.clipboardData) {
                content = (e.originalEvent || e).clipboardData.getData('text/plain');
                document.execCommand("insertText", false, content);
            }        
        })

        if(options["debug"]) this.debug();
    }

    setButtonActive(type){
        if(type in this.toolBtns){
            this.toolBtns[type].forEach(element => {
                element.classList.add("tB-toolButton-active");        
            });
        }
    }

    setButtonInactive(type){
        if(type in this.toolBtns){
            this.toolBtns[type].forEach(element => {
                element.classList.remove("tB-toolButton-active");        
            });
        }
    }

    emptyTextbox(){
        this.editor.innerHTML = '<p style="text-align: left;"><br></p>'
    }

    focus(){
        this.editor.focus();
    }

    
    debug(){
        //this.editor.children[0].innerHTML = "this is <b>test</b>";

        
        //this.editor.children[0].innerText = "this is <b>test</b>";
    }

    empty(){

    }

    deleteSectionLocal(pIndex, startIndex, endIndex){
        //let originalStr = this.editor.children[pIndex].innerHTML;
        //this.editor.children[pIndex].innerHTML = originalStr.slice(0, insertIndex) + text + originalStr.slice(insertIndex);

        //local
        let focusedP = this.elementContent[pIndex];
        //let tags = focusedP.innerHTML.slice(startIndex, endIndex).match(/\<(.*?)\>/g);
        //let excludedTags;
        //if(tags != null) excludedTags = this.oddTagMatches(tags)
        //else excludedTags = [""]

        focusedP.innerHTML = this.removeTags(focusedP.innerHTML.slice(0, startIndex) + focusedP.innerHTML.slice(endIndex));
        //console.log("local: " + focusedP.innerHTML)
        this.onChange("delete", pIndex, startIndex, endIndex)
    }

    deleteSection(pIndex, startIndex, endIndex){
        //let originalStr = this.editor.children[pIndex].innerHTML;
        //this.editor.children[pIndex].innerHTML = originalStr.slice(0, insertIndex) + text + originalStr.slice(insertIndex);
        let sel = document.getSelection().getRangeAt(0);

        //local
        let focusedP = this.editor.children[pIndex];
        let tags = focusedP.innerHTML.slice(startIndex, endIndex).match(/\<(.*?)\>/g);
        let excludedTags;
        if(tags != null) excludedTags = this.oddTagMatches(tags)
        else excludedTags = [""]

        focusedP.innerHTML = this.removeEmptyTags(focusedP.innerHTML.slice(0, startIndex) + excludedTags.join("") + focusedP.innerHTML.slice(endIndex));
        this.elementContent[pIndex].innerHTML = this.removeTags(focusedP.innerHTML);
        focusedP.innerHTML.slice(0, startIndex) 
        
        this.setCaretPos("textIndex", pIndex, null, this.removeTags(focusedP.innerHTML.slice(0, startIndex)).length);
    }

    insertTextLocal(pIndex, insertIndex, text){
        let focusedP = this.elementContent[pIndex];
        
        if(focusedP) focusedP.innerHTML = focusedP.innerHTML.slice(0, insertIndex) + text + focusedP.innerHTML.slice(insertIndex);

        this.onChange("insert", pIndex, insertIndex, text);
        //console.log("local: " + focusedP.innerHTML)
    }

    removeTags(string){
        return string.replace(/(<([^>]+)>)/ig, "")
    }

    /**
     * get the range of **plain text from start to sel pos
     */
    getSelectionPos(){
        let _range = document.getSelection().getRangeAt(0);
        //which paragraph
        let focusedParagraph = _range.endContainer;
        if(focusedParagraph.tagName) while(focusedParagraph.nodeType == Node.TEXT_NODE || focusedParagraph.tagName.toLowerCase() != "p") focusedParagraph = focusedParagraph.parentNode;
        let pIndex = Array.prototype.indexOf.call(this.editor.children, focusedParagraph);

        let fOffsetRange = document.createRange();
        fOffsetRange.selectNode(focusedParagraph);
        fOffsetRange.setEnd(_range.endContainer, _range.endOffset);

        //get html of selection
        let space = document.createElement("div");
        space.appendChild(fOffsetRange.cloneContents());
        let selFromStart = this.removeTags(space.innerHTML);
        //selFromStart.length is insertIndex for html text for the paragraph
        
        return {"pIndex": pIndex, "selFromStart": selFromStart}
    }

    pushHTML(htmlString){
        this.editor.focus();
        var selObj = window.getSelection();
        var selRange = selObj.getRangeAt(0);

        selRange.deleteContents();   
        var parser = new DOMParser();
        var doc = parser.parseFromString(htmlString, 'text/html');
        let insertElement = document.createTextNode(" ");
        selRange.insertNode(insertElement);
        selRange.insertNode(doc.body.children[0]);

        
        var newRange = document.createRange();
        newRange.setStart(insertElement, 1);
        newRange.setEnd(insertElement, 1);
        selObj.removeAllRanges();
        selObj.addRange(newRange);
    }

    pushHtmlSingle(htmlString){
        this.editor.focus();
        var selObj = window.getSelection();
        var selRange = selObj.getRangeAt(0);
        selRange.deleteContents();   
        var parser = new DOMParser();
        var doc = parser.parseFromString(htmlString, 'text/html');     
        let insertElement = doc.body.children[0]
        selRange.insertNode(document.createTextNode(" "))
        selRange.insertNode(insertElement);
        //let nextSibling = insertElement.nextSibling;
        //let newNode;
        //console.log(nextSibling);
        //if(nextSibling){
        //    newNode = document.createTextNode(" ")
        //    insertElement.parentElement.insertBefore(newNode, nextSibling);
        //    
        //}else{
        //    newNode = document.createTextNode("-")
        //    insertElement.parentElement.appendChild(newNode);
        //}
        //var newRange = document.createRange();
        //newRange.setStart(newNode, 1);
        //newRange.setEnd(newNode, 1);
        //selObj.removeAllRanges();
        //selObj.addRange(newRange);
    }
    

    insertText(pIndex, insertIndex, htmlText){
        let focusedP = this.editor.children[pIndex];
        focusedP.innerHTML = focusedP.innerHTML.slice(0, insertIndex) + htmlText + focusedP.innerHTML.slice(insertIndex);
        this.elementContent[pIndex].innerHTML = this.removeTags(focusedP.innerHTML);
    }

    setCaretPos(type, pIndex, node, index, sel2){
        let sel = document.getSelection();
        let moveRange = document.createRange();
        switch(type){
        case "htmlIndex":
            moveRange.setStart(node, index);
            moveRange.collapse(true);
            sel.removeAllRanges();
            sel.addRange(moveRange);
            break;
        case "textIndex":
            let offset = getNodeFromOffset(pIndex, index);
            if(offset[0] != null){
                moveRange.setStart(offset[0], offset[1]);
                moveRange.collapse(true);
                sel.removeAllRanges();
                sel.addRange(moveRange);
            }
            break;
        case "withSelPos":
            sel.removeAllRanges();
            sel.addRange(sel2);
            break;
        case "withSelOrig":
            sel.removeAllRanges();
            sel.addRange(sel2);
            break;
        }
    }

    /**
     * This is a description of the function
     * @param {string} tag - tag that you want to wrap with
     */
    toggleTagLocal(tag, pIndex, startIndex, endIndex){
        //check if it is within the tag already
        let range = document.createRange();
        let startOffset = this.getNodeFromOffsetLocal(pIndex, startIndex)
        let endOffset = this.getNodeFromOffsetLocal(pIndex, endIndex);

        console.logconsole.log(startOffset, endOffset)
        
        if(startOffset[0] && endOffset[0]){
            range.setStart(startOffset[0], startOffset[1])
            range.setEnd(endOffset[0], endOffset[1])

            let ifAllBold = true;
            let childNodes = range.cloneContents().childNodes
            console.log(range.cloneContents().childNodes)
            let node;
            for(let i = 0; i < childNodes.length; i++){
                node = childNodes[i]
                console.dir(node)
                console.log(node.tagName, tag.toUpperCase())
                if(node.parentNode.tagname == undefined || node.parentNode.tagName != tag.toUpperCase()){
                    ifAllBold = false;
                    break;
                }
            }

            let rangeContent
            if(ifAllBold){
                rangeContent = range.cloneRange();
                range.deleteContents();
                for(node in rangeContent.childNodes){
                    console.log(node);
                    range.insertNode(node.childNodes[0])
                }
            }else{
                rangeContent = range.extractContents()
                let tagElement = document.createElement(tag)
                tagElement.appendChild(rangeContent)
                range.deleteContents()
                console.log(tagElement);
                range.insertNode(tagElement)
            }
        }

        console.log(this.elementContent[pIndex].innerHTML)
    }

    removeEmptyTags(string){
        
        let index = string.indexOf("><");
        while(index != -1){
            let startBraketIndex = string.lastIndexOf("<", index);
            let endBraketIndex = string.indexOf(">", index+2);
            let firstTag = string.substring(startBraketIndex + 1, index);
            let secondTag = string.substring(index + 2, endBraketIndex);

            if("/"+firstTag == secondTag) string = string.slice(0, startBraketIndex) + string.slice(endBraketIndex+1);
            
            index = string.indexOf("><", index+2);
        }

        return string;
    }

    oddTagMatches(array) {
        var values = [];
        var returnValues = []
        for (var i = 0; i < array.length; ++i) {
            var value = array[i].replace("/", "");
            let indexCache = values.indexOf(value);
            if (indexCache !== -1) {
                values.splice(indexCache);
                returnValues.splice(indexCache);
            }else {
                values.push(value);
                returnValues.push(value);
            }
        }
        return returnValues;
    }

    getNodeFromOffset(pIndex, index){
        let nodeList = this.editor.children[pIndex].childNodes;
        let offsetLimit = 0;
        for(let i = 0; i < nodeList.length; i++){
            let nodeF = nodeList[i];
            while(nodeF.nodeType != Node.TEXT_NODE) nodeF = nodeF.childNodes[0];
            if(offsetLimit + nodeF.data.length >= index){
                return [nodeF, index - offsetLimit];
            }
            offsetLimit += nodeF.data.length;
        }
        return [null, -1];
    }

    getNodeFromOffsetLocal(pIndex, index){
        let nodeList = this.elementContent[pIndex].childNodes;
        let offsetLimit = 0;
        for(let i = 0; i < nodeList.length; i++){
            let nodeF = nodeList[i];
            while(nodeF.nodeType != Node.TEXT_NODE) nodeF = nodeF.childNodes[0];
            if(offsetLimit + nodeF.data.length >= index){
                return [nodeF, index - offsetLimit];
            }
            offsetLimit += nodeF.data.length;
        }
        return [null, -1];
    }

    getSelectionNative(){
        
        let sel = document.getSelection().getRangeAt(0);
        //which paragraph
        let focusedParagraph = sel.endContainer;
        while(focusedParagraph.nodeType == Node.TEXT_NODE || focusedParagraph.tagName.toLowerCase() != "p") focusedParagraph = focusedParagraph.parentNode;
        let pIndex = Array.prototype.indexOf.call(this.editor.children, focusedParagraph);
        
        let selRange = document.createRange();
        selRange.selectNode(focusedParagraph);
        selRange.setEnd(sel.startContainer, sel.startOffset);
        
        //get html of selection
        let space = document.createElement("div");
        space.appendChild(selRange.cloneContents());
        let selFromStart = this.removeTags(space.innerHTML);

        let startIndex = selFromStart.length;

        selRange.setEnd(sel.endContainer, sel.endOffset);
        //get html of selection
        space = document.createElement("div");
        space.appendChild(selRange.cloneContents());
        selFromStart = this.removeTags(space.innerHTML);

        let endIndex = selFromStart.length;

        return [pIndex, startIndex, endIndex];
    }

    getSelectionPureTextIndex(){
        let sel = document.getSelection().getRangeAt(0);
        //which paragraph
        let focusedParagraph = sel.endContainer;
        while(focusedParagraph.nodeType == Node.TEXT_NODE || focusedParagraph.tagName.toLowerCase() != "p") focusedParagraph = focusedParagraph.parentNode;
        let pIndex = Array.prototype.indexOf.call(this.editor.children, focusedParagraph);
        
        let selRange = document.createRange();
        selRange.selectNode(focusedParagraph);
        selRange.setEnd(sel.startContainer, sel.startOffset);
        
        //get html of selection
        let space = document.createElement("div");
        space.appendChild(selRange.cloneContents());
        let selFromStart = this.removeTags(space.innerHTML);
        while(selFromStart.slice(-1) == ">") selFromStart = selFromStart.substring(0, selFromStart.lastIndexOf("</"));

        let startIndex = selFromStart.length;

        selRange.setEnd(sel.endContainer, sel.endOffset);
        //get html of selection
        space = document.createElement("div");
        space.appendChild(selRange.cloneContents());
        selFromStart = this.removeTags(space.innerHTML);
        let endIndex = selFromStart.length;

        return [pIndex, startIndex, endIndex];
    }

    getText(){
        return this.editor.innerText;
    }

    getHtml(){
        return this.editor.innerHTML;
    }

}

function getCssFile(url){
    var link = document.createElement( "link" );
    link.href = url;
    link.type = "text/css";
    link.rel = "stylesheet";

    document.getElementsByTagName( "head" )[0].appendChild( link );
}