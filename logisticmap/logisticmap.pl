%====================================================================================
% logisticmap description   
%====================================================================================
system(logisticmap).   %%[Claude, Nov 2026] CONTEXT INFO
request( check, check(V) ).
reply( checkanswer, checkanswer(C) ).  %%for check
request( checkloc, check(V) ).  %synchLocal
reply( checklocreply, answer(V) ).  %%for checkloc (synchLocal)
%%--- CONTEXT INFO: msgdoc/2 replyfor/2 internalmsg/1
replyfor( checkanswer, check ).
replyfor( checklocreply, checkloc ).
%====================================================================================
context(ctxlogisticmap, "localhost",  "TCP", "8080").
 qactor( a0, ctxlogisticmap, "it.unibo.a0.A0").
 static(a0).
  qactor( a1, ctxlogisticmap, "it.unibo.a1.A1").
 static(a1).
