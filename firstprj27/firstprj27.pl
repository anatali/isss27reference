%====================================================================================
% firstprj27 description   
%====================================================================================
request( evalfun, arg(V) ).
reply( evalreply, value(V) ).  %%for evalfun
request( evalfunvalues, args(MIN,MAX,DX) ).
reply( replyvalues, values(S) ).  %%for evalfunvalues
%====================================================================================
context(ctxfirstprj27, "localhost",  "TCP", "8120").
 qactor( a, ctxfirstprj27, "it.unibo.a.A").
 static(a).
  qactor( callerforquicktesting, ctxfirstprj27, "it.unibo.callerforquicktesting.Callerforquicktesting").
 static(callerforquicktesting).
