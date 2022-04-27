# DESARROLLO DE UNA APLICACIÓN MÓVIL CONTRA EL SECUESTRO DE GRABACIONES DE VÍDEO PRIVADAS

<p>Esta aplicación tiene como objetivo proteger y asegurar las grabaciones privadas, de forma que no se puedan visualizar sin consentimiento o ser 
  eliminadas de manera definitiva por error. Para conseguirlo, tiene el siguiente funcionamiento: </p>
  
  <ol>
  <li> Grabación del vídeo </li>
  <li> Etapa de cifrado </li>
  <li> Almacenamiento en varios servicios web </li>
  </ol>
  
  ## Cifrado
  <p> Esta aplicación empleará un cifrado híbrido. Los algoritmos utilizados para ello son AES/GCM y RSA. Finalmente se exportará una clave privada, en un certificado
  PKCS#8, con un cifrado PBE (con una contraseña elegida por el usuario). </p>
  
  ## Almacenamiento
  <p> Los servicios web que se utilizan son Google Drive, AnonFiles y File.io, de forma que se proporcionan varios servicios en los cuales almacenar la grabación para
  mantenerla segura </p>
