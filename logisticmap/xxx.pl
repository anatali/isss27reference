%====================================================================================
% xxx description   
%====================================================================================
request( check, check(V) ).
reply( checkanswer, checkanswer(C) ).  %%for check
request( checkloc, check(V) ).  %synchLocal
reply( checklocreply, answer(V) ).  %%for checkloc (synchLocal)
%====================================================================================
context(ctxxxx, "localhost",  "TCP", "8080").
 qactor( a0, ctxxxx, "it.unibo.a0.A0").
 static(a0).
  qactor( a1, ctxxxx, "it.unibo.a1.A1").
 static(a1).
