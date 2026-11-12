%====================================================================================
% firstprj27 description   
%====================================================================================
system(firstprj27).   %%[Claude, Nov 2026] CONTEXT INFO
request( evalfun, arg(V) ).
reply( evalreply, value(V) ).  %%for evalfun
request( evalfunvalues, args(MIN,MAX,DX) ).
reply( replyvalues, values(S) ).  %%for evalfunvalues
%%--- CONTEXT INFO: msgdoc/2 replyfor/2 internalmsg/1
replyfor( evalreply, evalfun ).
replyfor( replyvalues, evalfunvalues ).
%====================================================================================
context(ctxfirstprj27, "localhost",  "TCP", "8120").
 qactor( a, ctxfirstprj27, "it.unibo.a.A").
 static(a).
