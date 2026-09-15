%====================================================================================
% firstprj27 description   
%====================================================================================
request( evalfunvalues, args(MIN,MAX,DX) ).
reply( replyvalues, values(S) ).  %%for evalfunvalues
dispatch( coapUpdate, changed(TERM) ).
dispatch( evalued, changed(TERM) ).
%====================================================================================
context(ctxfirstprj27, "localhost",  "TCP", "8120").
 qactor( a, ctxfirstprj27, "it.unibo.a.A").
 static(a).
  qactor( coapobserver, ctxfirstprj27, "it.unibo.coapobserver.Coapobserver").
 static(coapobserver).
