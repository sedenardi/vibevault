<?php
/* JSON parsing function with field names */
function mysql2json($mysql_result,$name){
     $json="{\n\"$name\": [\n";
     $field_names = array();
     $fields = mysql_num_fields($mysql_result);
     for($x=0;$x<$fields;$x++){
          $field_name = mysql_fetch_field($mysql_result, $x);
          if($field_name){
               $field_names[$x]=$field_name->name;
          }
     }
     $rows = mysql_num_rows($mysql_result);
     for($x=0;$x<$rows;$x++){
          $row = str_replace('"','\"',mysql_fetch_array($mysql_result));
          $json.="{\n";
          for($y=0;$y<count($field_names);$y++) {
               $json.="\"$field_names[$y]\" :	\"$row[$y]\"";
               if($y==count($field_names)-1){
                    $json.="\n";
               }
               else{
                    $json.=",\n";
               }
          }
          if($x==$rows-1){
               $json.="\n}\n";
          }
          else{
               $json.="\n},\n";
          }
     }
     $json.="]\n};";
     return($json);
}
?>