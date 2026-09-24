%====================================================================================
% logisticmap description   
%====================================================================================
%====================================================================================
context(ctxlogisticmap, "localhost",  "TCP", "8333").
 qactor( mapservice, ctxlogisticmap, "it.unibo.mapservice.Mapservice").
 static(mapservice).
