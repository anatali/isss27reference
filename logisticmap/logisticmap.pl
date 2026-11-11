%====================================================================================
% logisticmap description   
%====================================================================================
request( check, check(V) ).
reply( checkanswer, checkanswer(C) ).  %%for check
request( checkloc, check(V) ).  %synchLocal
reply( checklocreply, answer(V) ).  %%for checkloc (synchLocal)
%====================================================================================
context(ctxlogisticmap, "localhost",  "TCP", "8080").
 qactor( a0, ctxlogisticmap, "it.unibo.a0.A0").
 static(a0).
  qactor( a1, ctxlogisticmap, "it.unibo.a1.A1").
 static(a1).
