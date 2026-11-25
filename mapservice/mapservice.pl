%====================================================================================
% mapservice description   
%====================================================================================
system(mapservice).   %%[Claude, Nov 2026] CONTEXT INFO
request( evallogistic, args(R,X0,N) ). %richiesta della traiettoria della mappa logistica per (r, x0, N)
reply( logisticdone, done(URL) ). %%for evallogistic | successo: indirizzo della pagina con il grafico (D6)
reply( logisticfailed, failed(CAUSE) ). %%for evallogistic | fallimento con causa: wrongparams | unavailable | timeout (D10)
%%--- CONTEXT INFO: msgdoc/2 replyfor/2 internalmsg/1
msgdoc( evallogistic, 'richiesta della traiettoria della mappa logistica per (r, x0, N)' ).
replyfor( logisticdone, evallogistic ).
msgdoc( logisticdone, 'successo: indirizzo della pagina con il grafico (D6)' ).
replyfor( logisticfailed, evallogistic ).
msgdoc( logisticfailed, 'fallimento con causa: wrongparams | unavailable | timeout (D10)' ).
%====================================================================================
context(ctxmapservice, "localhost",  "TCP", "8333").
 qactor( mapservice, ctxmapservice, "it.unibo.mapservice.Mapservice").
 static(mapservice).
  qactor( clientsim, ctxmapservice, "it.unibo.clientsim.Clientsim").
 static(clientsim).
