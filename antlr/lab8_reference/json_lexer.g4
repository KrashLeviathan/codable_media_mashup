lexer grammar json_lexer;

@lexer::members {
  String errorString = "";
  String lastTag = "";
  String emailString = "";
  String dateString = "";
  String phoneString = "";
  String cardString = "";
  String otherString = "";

  public static void print(String label, String value) {
     System.out.println(String.format("%1$-14s", label) + ":  " + value);
  }

  static String removeWS(String str) {
      return str.replaceAll("[ \t\r\n]+","");
  }

  void handleError(int level, String tag) {
      if (errorString.length() > 0) {
          if (tag != null) {
              errorString = "\"" + tag + "\": \"" + errorString;
          }
          print("    BAD INPUT", errorString);
      }
      errorString = "";
      for (int i=0; i<level; i++) {
          popMode();
      }
  }

  void dumpErrors() {
      handleError(0, null);
  }

  void handleCardMatch() {
      if (errorString.length() > 0) {
          errorString += getText();
      } else {
          cardString = getText();
      }
  }
}

fragment OK_TAG_NAME
    :    (ALPHA | '_') (ALNUM | '-' | '_' | '.')*
    ;
fragment MONTH
    :    ('01'|'02'|'03'|'04'|'05'|'06'|'07'|'08'|'09'|'10'|'11'|'12')
    ;
fragment DAY
    :    ('01'|'02'|'03'|'04'|'05'|'06'|'07'|'08'|'09'|'10'|
        '11'|'12'|'13'|'14'|'15'|'16'|'17'|'18'|'19'|'20'|
        '21'|'22'|'23'|'24'|'25'|'26'|'27'|'28'|'29'|'30'|'31')
    ;
fragment ALNUM
    :    ALPHA | DIGIT
    ;
fragment ALPHA
    :    [a-zA-Z]
    ;
fragment DIGIT
    :    [0-9]
    ;
fragment VALID_SPECIAL
    :    [-_~!$&'()*+,;=:]
    ;
fragment WS
    :    [ \t\r\n]
    ;


// Starting Mode (looking for an opening bracket)
END_OF_FILE
    :    . EOF
    {
        if (errorString.length() > 0) {
            errorString += getText();
            handleError(0, null);
        } else if (!getText().equals("\r") && !getText().equals("\n")) {
            if (getText().equals("}")) {
                // TODO
                System.out.println("Found closing bracket");
            } else {
                errorString += getText();
                handleError(0, null);
            }
        }
        System.out.println("End of File");
    }
    ;
START
    :    '{' WS* '"'
    {
        if (errorString.length()==0) {
            System.out.println("**************   Start new object   **************\n");
            pushMode(OPEN_TAG_MODE);
        } else {
            errorString += getText();
        }
    }
    ;
WS_SKIPPED
    :    WS  -> skip
    ;
ERROR_CATCHING_DEFAULT
    :    ~('{')
    {
        errorString += getText();
    }
    ;



// ###########################################################################

mode OPEN_TAG_MODE;
EMAIL_TAG
    :    [Ee][Mm][Aa][Ii][Ll]'":' WS* '"'
        {
            dumpErrors();
            lastTag = "EMAIL";
            pushMode(EMAIL_MODE);
        }
    ;
DATE_TAG
    :    [Dd][Aa][Tt][Ee]'":' WS* '"'
        {
            dumpErrors();
            lastTag = "DATE";
            pushMode(DATE_MODE);
        }
    ;
PHONE_TAG
    :    [Pp][Hh][Oo][Nn][Ee]'":' WS* '"'
        {
            dumpErrors();
            lastTag = "PHONE";
            pushMode(PHONE_MODE);
        }
    ;
CARD_TAG
    :    [Cc][Rr][Ee][Dd][Ii][Tt][Cc][Aa][Rr][Dd]'":' WS* '"'
        {
            dumpErrors();
            lastTag = "CREDITCARD";
            pushMode(CARD_MODE);
        }
    ;
OTHER_TAG
    :    OK_TAG_NAME '":' WS* '"'
        {
            if (errorString.length() > 0) {
                errorString = "\"" + errorString + removeWS(getText());
                dumpErrors();
            } else {
                lastTag = getText().toUpperCase().split("\"")[0];
                if (lastTag.length() > 2
                    && lastTag.substring(0,3).equals("XML")) {
                    errorString += "\"" + removeWS(getText());
                } else {
                    dumpErrors();
                    otherString = "";
                    pushMode(OTHER_MODE);
                }
            }
        }
    ;
CLOSE_TAG
    :    WS* '}' WS*
        {
            if (errorString.length() > 0) {
                errorString += "}";
            } else {
                dumpErrors();
                System.out.println();
                popMode();
            }
        }
    ;
ERROR_CLOSE
    :    '"' ','? WS* '"'?
    {
        errorString += removeWS(getText());
        dumpErrors();
    }
    ;
TAG_ERROR_CATCHING
    :    ~('}')
        {
            errorString += getText();
        }
    ;




// ###########################################################################

mode EMAIL_MODE;
EMAIL
    :    LOCAL_PART* LOCAL_PART_MINUS '@' DOMAIN_PART+
    {
        if (errorString.length() > 0) {
            errorString += getText();
        } else {
            emailString = getText();
        }
    }
    ;
fragment LOCAL_PART
    :    LOCAL_PART_MINUS [.]?
    ;
fragment LOCAL_PART_MINUS
    :    ALNUM | VALID_SPECIAL
    ;
fragment DOMAIN_PART
    :    ALNUM | '-' |'.'
    ;
EMAIL_CLOSE
    :    '"' ','? WS* '"'?
    {
        if (errorString.length() > 0) {
            errorString += removeWS(getText());
            emailString = "";
            handleError(1, "EMAIL");
        } else {
            print("EMAIL", emailString);
            emailString = "";
            popMode();
        }
    }
    ;
EMAIL_ERROR_CATCHING
    :    ~('"')
    {
        if (emailString.length() > 0) {
            errorString += emailString;
            emailString = "";
        }
        errorString += getText();
    }
    ;




mode DATE_MODE;
fragment DATE_DELIM
    :    (' ' | '/' | '-')
    ;
DATE
    :    DAY DATE_DELIM MONTH DATE_DELIM (('20' DIGIT DIGIT) | '2100')
        {
            if (errorString.length() > 0) {
                errorString += getText();
            } else {
                dateString += getText();;
            }
        }
    ;
DATE_CLOSE
    :    '"' ','? WS* '"'?
    {
        if (errorString.length() > 0) {
            errorString += removeWS(getText());
            dateString = "";
            handleError(1, "DATE");
        } else {
            print("DATE", dateString);
            dateString = "";
            popMode();
        }
    }
    ;
DATE_ERROR_CATCHING
    :    ~('"')
    {
        if (dateString.length() > 0) {
            errorString += dateString;
            dateString = "";
        }
        errorString += getText();
    }
    ;



mode PHONE_MODE;
PHONE_W_PARENS
    :    '(' THREE_DIGITS ') ' THREE_DIGITS '-' THREE_DIGITS DIGIT
    {
        if (errorString.length() > 0) {
            errorString += getText();
        } else {
            phoneString = getText();
        }
    }
    ;
PHONE_WO_PARENS
    :    THREE_DIGITS PHONE_DELIM THREE_DIGITS PHONE_DELIM THREE_DIGITS DIGIT
    {getText().charAt(3)==getText().charAt(7)}?
    {
        if (errorString.length() > 0) {
            errorString += getText();
        } else {
            phoneString = getText();
        }
    }
    ;
PHONE_CLOSE
    :    '"' ','? WS* '"'?
    {
        if (errorString.length() > 0) {
            errorString += removeWS(getText());
            phoneString = "";
            handleError(1, "PHONE");
        } else {
            print("PHONE", phoneString);
            phoneString = "";
            popMode();
        }
    }
    ;
PHONE_ERROR_CATCHING
    :    ~('"')
    {
        if (phoneString.length() > 0) {
            errorString += phoneString;
            phoneString = "";
        }
        errorString += getText();
    }
    ;
fragment PHONE_PARENS
    :    '(' THREE_DIGITS ') ' THREE_DIGITS '-' THREE_DIGITS DIGIT
    ;
fragment THREE_DIGITS
    :    DIGIT DIGIT DIGIT
    ;
fragment PHONE_DELIM
    :    '-' | ' ' | '.'
    ;



mode CARD_MODE;
fragment CCD
    :    ' ' | '-'
    ;
VISA
    :    '4' DIGIT DIGIT DIGIT CCD?
                       DIGIT DIGIT DIGIT DIGIT CCD?
                       DIGIT DIGIT DIGIT DIGIT CCD?
                       DIGIT (DIGIT DIGIT DIGIT)?
    { handleCardMatch(); }
    ;
MASTERCARD
    :    '5' ('1'..'5') DIGIT DIGIT CCD?
                       DIGIT DIGIT DIGIT DIGIT CCD?
                       DIGIT DIGIT DIGIT DIGIT CCD?
                       DIGIT DIGIT DIGIT DIGIT
    { handleCardMatch(); }
    ;
AMER_EXPR
    :    '3' ('4' | '7') DIGIT DIGIT CCD?
                       DIGIT DIGIT DIGIT DIGIT CCD?
                       DIGIT DIGIT DIGIT DIGIT CCD?
                       DIGIT DIGIT DIGIT
    { handleCardMatch(); }
    ;
DINERS_CLUB
    :    (( '30' ('0'..'5') ) | ( ('36' | '38') DIGIT)) DIGIT CCD?
                       DIGIT DIGIT DIGIT DIGIT CCD?
                       DIGIT DIGIT DIGIT DIGIT CCD?
                       DIGIT DIGIT
    { handleCardMatch(); }
    ;
DISCOVER
    :    ('6011' | ('65' DIGIT DIGIT)) CCD?
                       DIGIT DIGIT DIGIT DIGIT CCD?
                       DIGIT DIGIT DIGIT DIGIT CCD?
                       DIGIT DIGIT DIGIT DIGIT
    { handleCardMatch(); }
    ;
JCB_A
    :    ('2131' | '1800') CCD?
                       DIGIT DIGIT DIGIT DIGIT CCD?
                       DIGIT DIGIT DIGIT DIGIT CCD?
                       DIGIT DIGIT DIGIT
    { handleCardMatch(); }
    ;
JCB_B
    :    '35' DIGIT DIGIT CCD?
                       DIGIT DIGIT DIGIT DIGIT CCD?
                       DIGIT DIGIT DIGIT DIGIT CCD?
                       DIGIT DIGIT DIGIT DIGIT
    { handleCardMatch(); }
    ;
CARD_CLOSE
    :    '"' ','? WS* '"'?
    {
        if (errorString.length() > 0) {
            errorString += removeWS(getText());
            cardString = "";
            handleError(1, "CREDITCARD");
        } else {
            print("CREDITCARD", cardString);
            cardString = "";
            popMode();
        }
    }
    ;
CARD_ERROR_CATCHING
    :    ~('"')
    {
        if (cardString.length() > 0) {
            errorString += cardString;
            cardString = "";
        }
        errorString += getText();
    }
    ;



mode OTHER_MODE;
OTHER
    :    ( ALNUM | VALID_SPECIAL | ' ' )+
        {
            if (errorString.length() > 0) {
                errorString += getText();
            } else {
                otherString = getText();
            }
        }
    ;
OTHER_CLOSE
    :    '"' ','? WS* '"'?
    {
        if (errorString.length() > 0) {
            errorString += removeWS(getText());
            otherString = "";
            handleError(1, lastTag);
        } else {
            print(lastTag, otherString);
            otherString = "";
            popMode();
        }
    }
    ;
OTHER_ERROR_CATCHING
    :    ~('"')
    {
        if (otherString.length() > 0) {
            errorString += otherString;
            otherString = "";
        }
        errorString += getText();
    }
    ;
