%====================================================================================
% firstprj27 description   
%====================================================================================
mqttBroker("localhost", "1883", "firstprj27rIn").
request( evalfun, arg(V) ).
reply( evalreply, value(V) ).  %%for evalfun
request( evalfunvalues, args(MIN,MAX,DX) ).
reply( replyvalues, values(S) ).  %%for evalfunvalues
event( serviceworking, serviceworking(V) ).
%====================================================================================
context(ctxfirstprj27, "localhost",  "TCP", "8120").
 qactor( a, ctxfirstprj27, "it.unibo.a.A").
 static(a).
