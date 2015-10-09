CREATE PROCEDURE `getShowsPrc`(
   userId      int,
   resultType  int,
   numResults  int,
   offset      int
)
BEGIN

set @rightNow = NOW();
set @userPK = userId;
set @lim = numResults;
Set @off = offset;

if (Select 1 from userTbl u where u.userId = @userPK) is null
   OR (@userPK = 0) Then
   Insert into userTbl(dateCreated)
   Select @rightNow;
   set @userPK = last_insert_id();
   Insert into activityTbl(userId,activityTypeId,activityDate,activityNotes)
   Select @userPK,1,@rightNow,'Created during getShows';
End If;


Insert into activityTbl(userId,activityTypeId,resultTypeId,numResults,activityDate,activityNotes)
Select @userPK,5,resultType,@lim,@rightNow,concat('Returning from offset: ',cast(@off as char));

if resultType = 1 then
Prepare STMT from 'Select *,? as userId from getShowAllTimeVw LIMIT ?,?';
elseif resultType = 2 then
Prepare STMT from 'Select *,? as userId from getShowDailyVw LIMIT ?,?';
elseif resultType = 3 then
Prepare STMT from 'Select *,? as userId from getShowWeeklyVw LIMIT ?,?';
elseif resultType = 4 then
Prepare STMT from 'Select *,? as userId from getShowNewestAddedVw LIMIT ?,?';
elseif resultType = 5 then
Prepare STMT from 'Select *,? as userId from getShowNewestVotedVw LIMIT ?,?';
else
Prepare STMT from 'Select *,? as userId from getShowAllTimeVw LIMIT ?,?';
end if;

EXECUTE STMT using @userPK,@off,@lim;

END