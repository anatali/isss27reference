%====================================================================================
% firstprj27 description   
%====================================================================================
request( evalfunvalues, arg(V) ).
reply( evalreply, values(V) ).  %%for evalfunvalues
%====================================================================================
context(ctxfirstprj27, "localhost",  "TCP", "8120").
 qactor( a, ctxfirstprj27, "it.unibo.a.A").
 static(a).
