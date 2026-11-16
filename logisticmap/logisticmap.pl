%====================================================================================
% logisticmap description   
%====================================================================================
system(logisticmap).   %%[Claude, Nov 2026] CONTEXT INFO
request( evallogistic, args(R,X1,Y) ).
reply( replylogistic, result(R) ).  %%for evallogistic
dispatch( showgraph, values(SOFPAIRS) ).
%%--- CONTEXT INFO: msgdoc/2 replyfor/2 internalmsg/1
replyfor( replylogistic, evallogistic ).
%====================================================================================
context(ctxlogisticmap, "localhost",  "TCP", "8333").
 qactor( mapservice, ctxlogisticmap, "it.unibo.mapservice.Mapservice").
 static(mapservice).
  qactor( viewer, ctxlogisticmap, "it.unibo.viewer.Viewer").
 static(viewer).
  qactor( callerforquicktesting, ctxlogisticmap, "it.unibo.callerforquicktesting.Callerforquicktesting").
 static(callerforquicktesting).
