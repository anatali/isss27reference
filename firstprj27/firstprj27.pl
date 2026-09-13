%====================================================================================
% firstprj27 description   
%====================================================================================
request( evalfunvalues, args(MIN,MAX,DX) ).
reply( replyvalues, values(S) ).  %%for evalfunvalues
event( starteval, starteval(MIN,MAX,DX) ).
event( stopeval, stopeval(V) ).
dispatch( coapUpdate, changed(TERM) ).
dispatch( evalued, changed(TERM) ).
%====================================================================================
context(ctxfirstprj27, "localhost",  "TCP", "8120").
 qactor( a, ctxfirstprj27, "it.unibo.a.A").
 static(a).
  qactor( coapobserver, ctxfirstprj27, "it.unibo.coapobserver.Coapobserver").
 static(coapobserver).
  qactor( callerforquicktesting, ctxfirstprj27, "it.unibo.callerforquicktesting.Callerforquicktesting").
 static(callerforquicktesting).
