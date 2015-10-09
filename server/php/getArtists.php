<?php
include 'config.php';
include 'mysql2json.php';

/* require the parameters */
if(isset($_GET['resultType']) && isset($_GET['numResults'])  && isset($_GET['offset']) 
	&& isset($_GET['userId'])) {

  /* declare type of results we are returning */
  $name = "artists";
  
  /* get in the passed variable and set to integers (prevents attacks implicitly) */
  $userId = intval($_GET['userId']);
  $resultType = intval($_GET['resultType']);
  $numResults = intval($_GET['numResults']) < 26 ? intval($_GET['numResults']) : 25;
  $offset = intval($_GET['offset']);
  
  /* grab the shows from the db */
  $query = "CALL getArtistsPrc('$userId','$resultType','$numResults','$offset')";
  $result = mysql_query($query) or die('Errant query:  '.$query);
 
  if(mysql_num_rows($result)) {
	header('Content-type: application/json');
    echo mysql2json($result,$name);
  }
  else{
	print('');
  }
  
  /* close connection */
  @mysql_close($link);
  
}
else{
	print('0');
}
?>
