%====================================================================================
% firstprj27 description   
%====================================================================================
system(firstprj27).   %%[Claude, Nov 2026] CONTEXT INFO
request( evalfun, arg(V) ). %valuta la funzione FSin (MyCode.FSin) nel punto V
reply( evalreply, value(V) ). %%for evalfun | V = valore della funzione nel punto richiesto
request( evalfunvalues, args(MIN,MAX,DX) ). %valuta la funzione nei punti da MIN a MAX con passo DX
reply( replyvalues, values(S) ). %%for evalfunvalues | S = stringa tra apici con i valori calcolati da MyCode.FSinSeries.evalSinPoints
%%--- CONTEXT INFO: msgdoc/2 replyfor/2 internalmsg/1
msgdoc( evalfun, 'valuta la funzione FSin (MyCode.FSin) nel punto V' ).
replyfor( evalreply, evalfun ).
msgdoc( evalreply, 'V = valore della funzione nel punto richiesto' ).
msgdoc( evalfunvalues, 'valuta la funzione nei punti da MIN a MAX con passo DX' ).
replyfor( replyvalues, evalfunvalues ).
msgdoc( replyvalues, 'S = stringa tra apici con i valori calcolati da MyCode.FSinSeries.evalSinPoints' ).
%====================================================================================
context(ctxfirstprj27, "localhost",  "TCP", "8120").
 qactor( a, ctxfirstprj27, "it.unibo.a.A").
 static(a).
