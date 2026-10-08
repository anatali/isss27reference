%====================================================================================
% logisticmap description   
%====================================================================================
request( evallogistic, args(R,X1,Y) ).
reply( replylogistic, result(R) ).  %%for evallogistic
dispatch( showgraph, values(SOFPAIRS) ).
%====================================================================================
context(ctxlogisticmap, "localhost",  "TCP", "8333").
 qactor( mapservice, ctxlogisticmap, "it.unibo.mapservice.Mapservice").
 static(mapservice).
  qactor( viewer, ctxlogisticmap, "it.unibo.viewer.Viewer").
 static(viewer).
  qactor( callerforquicktesting, ctxlogisticmap, "it.unibo.callerforquicktesting.Callerforquicktesting").
 static(callerforquicktesting).
