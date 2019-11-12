This program is a basic multi-threaded web server that is capable of serving pages to a web browser and can handle multiple connections. The program will setup a server on the local machine over port 8000. This can of course be modified for personal use. The server will service pages located in the specified directory. The directory can either be hard coded or passed as an argument when executing the program. Once connected with a client, the server will redirect them to an index.html file stored in the directory. The server will then service the clients requests.

At the moment, the program only supports GET, HEAD, and LIST requests.

To ensure that the program is working properly, one can either $telnet localhost PORT or connect to localhost:PORT through a browser. 
