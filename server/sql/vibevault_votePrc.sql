CREATE DEFINER=`sanders`@`%` PROCEDURE `votePrc`(
    id              int,
    showIdentifier  varchar(100),
    showArtist      varchar(100),
    showTitle       varchar(100),
    showDate        varchar(50),
    showSource      varchar(400),
    showRating      varchar(5)
)
BEGIN

Set @rightNow = NOW();
Set @resultText = 'Vote failed';
set @userPK = id;
set @artistPK = 0;
set @showPK = 0;

if (Select 1 from userTbl where userId = @userPK) is null
    OR (@userPK = 0) Then
    Insert into userTbl(dateCreated)
    Select @rightNow;
    Set @userPK = last_insert_id();
    Insert into activityTbl(userId,activityTypeId,activityDate,activityNotes)
    Select @userPK,1,@rightNow,'Created during vote';
End If;

if (ifnull(showIdentifier,'') like '') OR
    (ifnull(showArtist,'') like '') OR
    (ifnull(showTitle,'') like '') then
    Insert into activityTbl(userId,activityTypeId,activityDate,activityNotes)
    Select @userPK,8,@rightNow,concat(concat(concat(concat(concat('Bad vote - Identifer: ',ifnull(showIdentifier,'')),' Artist: '),ifnull(showArtist,'')),' Title: '),ifnull(showTitle,''));
    Select 'Invalid show' into @resultText;
else
    if (Select 1 from artistTbl where artist like showArtist) is null Then
        Insert into artistTbl(artist,dateCreated)
        Select showArtist,@rightNow;
        Set @artistPK = last_insert_id();
        Insert into activityTbl(userId,activityTypeId,activityDate,activityNotes)
        Select @userPK,2,@rightNow,concat('Created artist id: ',cast(@artistPK as char));
    else
        Select artistId into @artistPK
        from artistTbl where artist like showArtist;
    End If;


    if (Select 1 from showTbl where identifier like showIdentifier) is null Then
        Insert into showTbl(identifier,artistId,title,date,source,rating,dateCreated)
        Select showIdentifier,@artistPK,showTitle,showDate,showSource,showRating,@rightNow;
        Set @showPK = last_insert_id();
        Insert into activityTbl(userId,activityTypeId,activityDate,activityNotes)
        Select @userPK,3,@rightNow,concat('Created show id: ',cast(@showPK as char));
    else
        Select showId into @showPK
        from showTbl where identifier like showIdentifier;
        Update showTbl set rating = showRating where showId = @showPK;
    End If;


    if (Select 1 from voteTbl where userId = @userPK and showId = @showPK 
        and DATE(dateCreated) > DATE(SUBDATE(@rightNow,1))) is null Then
        Insert into voteTbl(userId,showId,dateCreated)
        Select @userPK,@showPK,@rightNow;
        Set @resultText = 'Vote successful';
        Insert into activityTbl(userId,activityTypeId,activityDate,activityNotes)
        Select @userPK,4,@rightNow,concat('Vote successful for show id: ',cast(@showPK as char));
    else
        Select 'Only one vote per show per day' into @resultText;
        Insert into activityTbl(userId,activityTypeId,activityDate,activityNotes)
        Select @userPK,4,@rightNow,concat('Vote not successful for show id: ',cast(@showPK as char));
    End If;
End If;

Select @userPK as 'userId', @resultText as 'resultText';

END