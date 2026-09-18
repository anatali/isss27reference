% Predicato ingresso
mappa_logistica(R, X, N, Risultato) :-
    mappa_acc(N, R, X, [X], Risultato).

% Caso base: N = 0, inverte accumulatore per avere ordine cronologico
mappa_acc(0, _, _, Acc, Risultato) :-
    mia_reverse(Acc, Risultato),
    stdout <- println( Risultato ).

    
% Caso ricorsivo
mappa_acc(N, R, X, Acc, Risultato) :-
    N > 0,
    X1 is R * X * (1 - X),
    N1 is N - 1,
    mappa_acc(N1, R, X1, [X1 | Acc], Risultato). 
    
mia_reverse(Lista, Invertita) :-
    mia_reverse(Lista, [], Invertita).

% mia_reverse(+ListaRimanente, +Accumulatore, -Risultato)
mia_reverse([], Acc, Acc).
mia_reverse([Testa|Coda], Acc, Risultato) :-
    mia_reverse(Coda, [Testa|Acc], Risultato).
