/**
    Author: Jakob Orel
    Date: 10/8/2021
    CSC315 Week 3 Playfair Cipher

    This program is designed to use the Playfair cipher to encrypt and decrypt messages that are read from a file in Scala.
    The message is read from a file 'input.txt', encrypted, and then the encoded message is written to a file 'output.txt'.
    A description of the Playfair cipher and an implementation can be found on the GeeksForGeeks website at
    https://www.geeksforgeeks.org/playfair-cipher-with-examples/
    This resource was used heavily during the development of this program. The example of using 'monarchy' as the key
    and 'instruments' as the message was used to prove correctness. It was also tested with examples like 'hello' and
    'helppo' to prove that double letter pairs were handled correctly. I also worked closely with Bram Dedrick and
    Nathan Daniels as we shared ideas to solve the problem.

    Throughout the program, I point out some of the useful (and sometimes not so useful) features of the language that
    I learned in comments. I also learned many different syntax including ommitting parentheses for functions with no
    arguments, ommitting the return keyword (a function simply returns the last value in the block), and a different
    syntax for using for loops. Some of the higher order functions that I found especially useful included distinct(),
    contains(), filter(), replace(), distinct(), and foreach(). The solution could be done using a map to store the
    character as a key and a tuple of integers to store the row and column in the key. I decided to use a
    multi-dimensional array and use higher order functions to search for the row and column in the key instead.
 **/

import java.io.{File, PrintWriter}
import scala.collection.mutable.ListBuffer
import scala.io.Source
import scala.util.control.Breaks._

class Playfair(val keyPhrase: String){

    /**
     * The key of the Playfair object is a 5x5 array of characters that contains a grid of the available 25 letters in
     * the order provided by a phrase.
     */
    val key: Array[Array[Char]] = createKeyGrid(keyPhrase)

    /**
     * Return true if the keyContains the character, false if not in key
     * Higher order functions were used to keep this function all in one line to test if a character exists in any of the rows.
     * Nathan was able to figure out this neat trick.
     * These higher order functions were something new I learned and can be very powerful as you can avoid many loops.
     * I did this previously using several for loops and a flag to check if the character was in each row.
     * @param char Character to check
     * @param key Key object to check if character is in
     * @return Boolean
     */
    private def keyContains(char: Char, key: Array[Array[Char]]): Boolean = if (key.exists(_.contains(char))) true else false

    /**
     * This function is used to add a character
     * Specific 'for loops' had to be used to get the correct index of where to insert the character into the key.
     * When originally initialized, the key has 'NUL' values in all positions.
     * A break statement is used to only insert into the first available position. This is a feature that is not available
     * in standard Scala and must be imported unlike many other languages. You must also include what you would like to
     * break out of in a 'breakable' block.
     * @param char Character to add
     * @param key Key to add the character to
     */
    private def addCharToKey(char: Char, key: Array[Array[Char]]): Unit ={
        breakable {
            for (row <- key) {
                for (index <- row.indices) {
                    // The empty keyGrid is a 5x5 array of 'NUL' characters that have to be replaced
                    if (row(index) == ' ') {
                        row(index) = char
                        break
                    }
                }
            }
        }
    } //addCharToKey

    /**
     * Creates the keyGrid (5x5) array using the keyContains and addCharToKey functions.
     * The key will be generated with each distinct character from the phrase being added from left to right and top
     * to bottom in the key grid. It will also add the rest of the characters that are remaining in the alphabet to the
     * key in alphabetical order of the remaining characters.
     * @param keyPhrase Phrase provided by user to determine the key
     * @return Multi-dimensional array as the key
     */
    private def createKeyGrid(keyPhrase: String): Array[Array[Char]] ={
        val keyGrid = Array.ofDim[Char](5, 5)

        // Using Leon's way of creating the alphabet string except 'J' using a loop.
        val alphabet = (for (letter <- 'A' to 'Z' if letter != 'J') yield letter).mkString

        // Create a string of distinct characters based on the keyPhrase and alphabet. Bram found this.
        val keyCharacters = (keyPhrase.toUpperCase.filter(_.isLetter).replace("J","I") + alphabet).toList.distinct.mkString

        // Iterate through keyPhrase with alphabet concatenated to add to keyGrid
        keyCharacters.foreach(phraseCh => {
            if (!keyContains(phraseCh, keyGrid)) {
                // Add character to next open position if not already in keyGrid
                addCharToKey(phraseCh, keyGrid)
            }
        })
        //return
        keyGrid
    } //createKeyGrid

    /**
     * This function creates a ListBuffer of strings that contain pairs of letters (digraphs) that do not have any
     * double letters and have no lone pairs.
     * @param plaintext message provided by user that will be encryped/decrypted
     * @return ListBuffer[String] containing the pairs of letters satisfying the rules for the Playfair Cipher.
     */
    private def createDigraphs(plaintext: String): ListBuffer[String] ={
        val digraphList = ListBuffer[String]()
        // Bram came up with this clever way to clean up the string all at once. (Capitalizing, filtering non-letters, and replacing 'J')
        val cleantext = plaintext.toUpperCase.filter(_.isLetter).replace("J", "I")

        // Add pairs of letters to a ListBuffer[String]
        // I used a while loop in order to decrement the index in the case that another character had to be inserted.
        var index = 0
        while (index < cleantext.length){
            // If it is the last lone character, then add 'Z' and add to list
            if(index > cleantext.length-2){
                digraphList += cleantext(index).toString + 'Z'
            }
            // If the pair are both X's then add a 'Z' as a bogus character
            else if(cleantext(index)=='X' && cleantext(index+1)=='X') {
                digraphList += cleantext(index).toString + 'Z'
                index -= 1
            }
            // For all other double letter pairs add a bogus letter of 'X'
            // If the pair is the same character add the first letter with bogus letter of 'X' then go back in index to get next letter
            else if(cleantext(index)==cleantext(index+1)) {
                digraphList += cleantext(index).toString + "X"
                index -= 1
            }
            // Add allowable pairs of letters to list of pairs
            else digraphList += cleantext(index).toString + cleantext(index+1).toString
            index += 2
        }
        //return
        digraphList
    } //createDigraphs

    /**
     * The cipher function is capable of both encrypting and decrypting messages using the playfair cipher. It uses the key
     * in the object of the Playfair class. Based on the value of the 'encrypt' boolean it will determine which way to
     * change the message. If you are encrypting, the incrementing through the key will increase, decrypting will decrease.
     * This also affects how the edges are wrapped around in the arrays.
     * @param plaintext message the user provides to encrypt/decrypt.
     * @param encrypt determines if the function will encrypt or decrypt plaintext
     * @return String of encrypted/decrypted message
     */
    def cipher(plaintext: String, encrypt: Boolean): String ={
        var increment = 0
        if (encrypt) increment = 1 else increment = -1
        val digraphList = createDigraphs(plaintext)
        var message = ""

        // For each pair in the ListBuffer of Strings (pairs)
        digraphList.foreach(digraph =>{
            // Initialize row and column variables
            var firstRow = 0
            var firstCol = -1
            var secondRow = 0
            var secondCol = -1
            // Using indexOf in each row returns a value for each row of where the character is.
            // If it does not exist in the row, a value of -1 is returned.
            // For every value of -1 that is returned the row variable is incremented.
            // If a value other than -1 is found (indicating the index in the row) it is returned as the column value.
            // Once the column value is set (meaning it is found in a row), the row value stops incrementing.
            // I learned also that I could not use an '_' character to do a comparison in the if and else if statements.
            // In simple cases, the '_' represents the value of what is being looped through in each foreach call, but
            // I was not able to use it in the if condition in this case. To solve this, I had to name the value that
            // was returned to 'value' and then use the '=>' operator to pass it to the if/else.
            key.map(_.indexOf(digraph(0))).foreach({ value =>
                if(value == -1 && firstCol == -1) firstRow += 1
                else if (value > -1) firstCol = value
            })
            key.map(_.indexOf(digraph(1))).foreach({ value =>
                if(value == -1 && secondCol == -1) secondRow += 1
                else if (value > -1) secondCol = value
            })

            // If same row, increment/decrement col of each, wrap around if needed.
            if (firstRow == secondRow){
                if (encrypt && firstCol == 4) firstCol = 0
                else if (!encrypt && firstCol == 0) firstCol = 4
                else firstCol += increment

                if (encrypt && secondCol == 4) secondCol = 0
                else if (!encrypt && secondCol ==0) secondCol = 4
                else secondCol += increment
            }
            // If same col, increment/decrement row of each, wrap around if needed.
            else if (firstCol == secondCol){
                if (encrypt && firstRow == 4) firstRow = 0
                else if (!encrypt && firstRow == 0) firstRow = 4
                else firstRow += increment

                if (encrypt && secondRow == 4) secondRow = 0
                else if (!encrypt && secondRow == 0) secondRow = 4
                else secondRow += increment
            }
            // If not same row and not same col, swap the column values
            else {
                var tempCol = 0
                tempCol = firstCol
                firstCol = secondCol
                secondCol = tempCol
            }
            // Because of the process of creating the pairs, there should be no cases where the pair has the same row
            // and the same column.

            // Now first and second rows/cols are the encrypted values I need to find in my key and add to encryptedMessage
            message += key(firstRow)(firstCol).toString + key(secondRow)(secondCol).toString
        })
        //return
        message.toLowerCase
    }
} //Playfair

object Playfair extends App{
    // Read file into message variable
    val messageFileName = getClass.getResource("input.txt")
    val messageFileSource = Source.fromFile(messageFileName.getFile)
    val message = messageFileSource.getLines.mkString
    messageFileSource.close

    println(s"File contents: $message")

    // Because this is a symmetric encryption, the same key is used for encryption and decryption of the message.
    val keyPhrase = "monarchy"
    // Create new Playfair object- this object contains the key given
    val playfair = new Playfair(keyPhrase)

    // Print key
    println("Key:")
    playfair.key foreach { row => println(row.mkString(" "))}

    // Create digraphs (letter pairs), only printing to show correctness of program
    val digraphs = playfair.createDigraphs(message)
    println(s"Digraph letter pairs: $digraphs")

    // Encrypt message
    val encryptedMessage = playfair.cipher(message, encrypt = true)
    println(s"Encrypted message: $encryptedMessage")

    // Decrypt message
    val decryptedMessage = playfair.cipher(encryptedMessage, encrypt = false)
    println(s"Decrypted message: $decryptedMessage")

    // Write encrypted message to new file, overwrites file of same name
    val encryptedFile = new PrintWriter(new File("src/main/resources/output.txt" ))
    encryptedFile.write(encryptedMessage)
    encryptedFile.close()

    /* OUTPUT:
    File contents: instruments
    Key:
    M O N A R
    C H Y B D
    E F G I K
    L P Q S T
    U V W X Z
    Digraph letter pairs: ListBuffer(IN, ST, RU, ME, NT, SZ)
    Encrypted message: gatlmzclrqtx
    Decrypted message: instrumentsz

    The output.txt file contains "gatlmzclrqtx".
     */
}
