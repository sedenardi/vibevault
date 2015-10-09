<?php
include 'config.php';
include 'mysql2json.php';

/* require the parameters */
if(isset($_GET['userId']) && isset($_GET['showIdent']) && isset($_GET['showArtist']) && isset($_GET['showTitle'])
	&& isset($_GET['showDate']) && isset($_GET['showSource']) && isset($_GET['showRating'])) {

	/* declare type of results we are returning */
	$name = "results";
  
	/* get in the passed variables & sanitize against injection */
	$userId = intval($_GET['userId']);
	$showIdent = mysql_real_escape_string($_GET['showIdent']);
	$showArtist = mysql_real_escape_string($_GET['showArtist']);
	$showTitle = mysql_real_escape_string($_GET['showTitle']);
	$showDate = mysql_real_escape_string($_GET['showDate']);
	$showSource = mysql_real_escape_string($_GET['showSource']);
	$showRating = mysql_real_escape_string($_GET['showRating']);

	/* call vote procedure */
	$query = "CALL votePrc('$userId','$showIdent','$showArtist','$showTitle','$showDate','$showSource','$showRating')";
	$result = mysql_query($query) or die('Errant query:  '.$query);
 
	if(mysql_num_rows($result)) {
		header('Content-type: application/json');
		echo mysql2json($result,$name);
	}
	else{
		print('');
	}
	@mysql_close($link);
}
else{
	print('0');
}