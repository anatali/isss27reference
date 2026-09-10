%====================================================================================
% firstprj27 description   
%====================================================================================
mqttBroker("localhost", "1883", "firstprj27rIn").
request( evalfunvalues, args(MIN,MAX,DX) ).
reply( replyvalues, values(S) ).  %%for evalfunvalues
dispatch( setParams, args(MIN,MAX,DX) ).
event( serviceworking, serviceworking(V) ).
event( starteval, starteval(MIN,MAX,DX) ).
event( stopeval, stopeval(V) ).
%====================================================================================
context(ctxfirstprj27, "localhost",  "TCP", "8120").
 qactor( a, ctxfirstprj27, "it.unibo.a.A").
 static(a).
  qactor( callerforquicktesting, ctxfirstprj27, "it.unibo.callerforquicktesting.Callerforquicktesting").
 static(callerforquicktesting).
