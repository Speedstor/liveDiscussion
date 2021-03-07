class S_WebSocket{
    constructor(socketUrl, options){
        this.socket = new WebSocket(socketUrl);
                            
        if(options){
            this.keepAlive = options.hasOwnProperty("keepAlive") ? options.keepAlive : false;
            this.onMessage = options.hasOwnProperty("onMessage") ? options.onMessage : this.empty;
        }

        this.socket.onclose = (event) => {
            console.log(event.data);
            console.log("notification socket closed !! -- -----")
            //re-establish websocket
            if(this.keepAlive){
                //socket = new WebSocket(socketUrl);
            }
        }

        this.socket.onmessage = (event) => {
            console.log("on message: "+event.data);

            var response = JSON.parse(event.data);

            this.onMessage(response);
        }

        this.syncing = false;

        return this.socket;
    }

    /**
     * Send the data to the websocket
     * 
     * @param {string} data 
     */
    send(data){
        console.log("Websocket send: "+data);
        let index = data.indexOf(" ");
        if(index > -1){
            let command = data.substring(0, index);
            if(command == "sync") this.syncing = true;
            else if(command == "endsync") this.syncing = false;
        }
        this.socket.send(data);
    }

    empty(...placeHolder){

    }
}

function createSocket(id){

}
