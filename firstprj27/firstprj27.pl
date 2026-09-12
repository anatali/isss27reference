%====================================================================================
% firstprj27 description   
%====================================================================================
mqttBroker("localhost", "1883", "firstprj27rIn").
request( evalfunvalues, args(MIN,MAX,DX) ).
reply( replyvalues, values(S) ).  %%for evalfunvalues
event( serviceelab, serviceelab(X,Y) ).
event( starteval, starteval(MIN,MAX,DX) ).
event( stopeval, stopeval(V) ).
%====================================================================================
context(ctxfirstprj27, "localhost",  "TCP", "8120").
 qactor( a, ctxfirstprj27, "it.unibo.a.A").
 static(a).
  qactor( perceiver, ctxfirstprj27, "it.unibo.perceiver.Perceiver").
 static(perceiver).
  qactor( callerforquicktesting, ctxfirstprj27, "it.unibo.callerforquicktesting.Callerforquicktesting").
 static(callerforquicktesting).
