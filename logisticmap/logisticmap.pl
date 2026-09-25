%====================================================================================
% logisticmap description   
%====================================================================================
request( evallogistic, args(MIN,MAX,DX) ).
reply( replylogistic, result(R) ).  %%for evallogistic
%====================================================================================
context(ctxlogisticmap, "localhost",  "TCP", "8333").
 qactor( mapservice, ctxlogisticmap, "it.unibo.mapservice.Mapservice").
 static(mapservice).
  qactor( viewer, ctxlogisticmap, "it.unibo.viewer.Viewer").
 static(viewer).
