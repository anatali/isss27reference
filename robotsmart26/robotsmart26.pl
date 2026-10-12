%====================================================================================
% robotsmart26 description   
%====================================================================================
request( tuneAtHome, tuneAtHome(X) ). %reposition in home X don't care
reply( tuneDone, tuneDone(X) ).  %%for tuneAtHome
dispatch( cmd, cmd(MOVE) ). %command for basic move l|r|a|d|h
dispatch( end, end(ARG) ). %termination of basicrobot
request( step, step(TIME) ). %step command for TIME duration
reply( stepdone, stepdone(V) ).  %%for step
reply( stepfailed, stepfailed(DURATION,CAUSE) ).  %%for step
request( buildPlan, buildPlan(PX,PY,TX,TY) ). %create plan from (PX,PY) to (TX,TY)
reply( buildPlanDone, buildPlanDone(PLAN) ).  %%for buildPlan
request( doplan, doplan(PATH,STEPTIME) ). %execute path PATH with STEPTIME
reply( doplandone, doplandone(ARG) ).  %%for doplan
reply( doplanfailed, doplanfailed(PLANTODO) ).  %%for doplan
dispatch( setrobotstate, setpos(X,Y,D) ). %set robot position to (X,Y) direction D=up|down|left|right
dispatch( setdirection, dir(D) ). %set robot direction to D=up|down|left|right
request( moverobot, moverobot(TARGETX,TARGETY,STEPTIME) ). %move from current pos to (TARGETX,TARGETY)
reply( moverobotdone, moverobotok(ARG) ).  %%for moverobot
reply( moverobotfailed, moverobotfailed(PLANDONE,PLANTODO) ).  %%for moverobot
request( getrobotstate, getrobotstate(ARG) ). %request robot state ARG unused
reply( robotstate, robotstate(POS,DIR) ). %%for getrobotstate | POS->pos(X,Y) DIR->up|down|left|right
request( getenvmap, getenvmap(X) ). %request environment map as string
reply( envmap, envmap(MAP) ). %%for getenvmap | MAP->string 
event( alarm, alarm(X) ). %alarm event
dispatch( setplanbuildelay, setplanbuildelay(V) ). %set PlanDBuildDelay parameter = V > 0
dispatch( noplan, noplan(X) ).
event( sonardata, sonar(DISTANCE) ).
event( obstacle, obstacle(X) ).
event( info, info(X) ).
dispatch( nextmove, nextmove(M) ).
dispatch( nomoremove, nomoremove(M) ).
%====================================================================================
context(ctxrobotsmart, "localhost",  "TCP", "8020").
 qactor( robotsmart, ctxrobotsmart, "it.unibo.robotsmart.Robotsmart").
 static(robotsmart).
