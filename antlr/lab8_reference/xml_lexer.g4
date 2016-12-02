lexer grammar xml_lexer;

@lexer::members {
  String errorString = "";
  String lastTag;
  String emailString = "";
  String dateString = "";
  String phoneString = "";
  String cardString = "";
  String otherString = "";

  public static void print(String label, String value) {
     System.out.println(String.format("%1$-14s", label) + ":  " + value);
  }

  void handleError(int level, String tag) {
      if (errorString.length() > 0) {
          if (tag != null) {
              errorString = "<" + tag + ">" + errorString;
          }
          print("    BAD INPUT", errorString);
      }
      errorString = "";
      for (int i=0; i<level; i++) {
          popMode();
      }
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


// Default Mode (For starting a fresh line)
OPEN
    :    '<' -> pushMode(OPEN_TAG_MODE)
    ;
ERROR_CATCHING_DEFAULT
    :    ~([\r\n] | '<')
    {
        errorString += getText();
    }
    ;
NEWLINE_DEFAULT
    :    [\r\n]+
        {
            handleError(0, null);
        }
    ;



// ###########################################################################

mode OPEN_TAG_MODE;
EMAIL_TAG
    :    [Ee][Mm][Aa][Ii][Ll]'>'
        {
            lastTag = "EMAIL";
            pushMode(EMAIL_MODE);
        }
    ;
DATE_TAG
    :    [Dd][Aa][Tt][Ee]'>'
        {
            lastTag = "DATE";
            pushMode(DATE_MODE);
        }
    ;
PHONE_TAG
    :    [Pp][Hh][Oo][Nn][Ee]'>'
        {
            lastTag = "PHONE";
            pushMode(PHONE_MODE);
        }
    ;
CARD_TAG
    :    [Cc][Rr][Ee][Dd][Ii][Tt][Cc][Aa][Rr][Dd]'>'
        {
            lastTag = "CREDITCARD";
            pushMode(CARD_MODE);
        }
    ;
OTHER_TAG
    :    OK_TAG_NAME '>'
        {
            if (errorString.length() > 0) {
                errorString += getText();
            } else {
                lastTag = getText().toUpperCase().substring(0,getText().length()-1);
                if (lastTag.length() > 2
                    && lastTag.substring(0,3).equals("XML")) {
                    errorString += "<" + getText();
                } else {
                    otherString = "";
                    pushMode(OTHER_MODE);
                }
            }
        }
    ;
CLOSE_TAG
    :    '/' OK_TAG_NAME '>'
        {
            if (errorString.length() > 0) {
                errorString += getText();
            } else {
                if (!getText().toUpperCase().equals("/" + lastTag + ">")) {
                    System.out.println(
                        "MISMATCHED CLOSING TAG:  <" + lastTag +
                        ">...<" + getText());
                    errorString = "";
                }
                popMode();
            }
        }
    ;
TAG_ERROR_CATCHING
    :    ~([\r\n])
        {
            if (errorString.length() == 0) {
                errorString += "<";
            }
            errorString += getText();
        }
    ;
NEWLINE_OTM
    :    [\r\n]+  {handleError(1, null);}
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
    :    '<'
    {
        if (errorString.length() > 0) {
            errorString += getText();
        } else {
            print("EMAIL", emailString);
            emailString = "";
            popMode();
        }
    }
    ;
EMAIL_ERROR_CATCHING
    :    ~([\r\n] | '<')
    {
        if (emailString.length() > 0) {
            errorString += emailString;
            emailString = "";
        }
        errorString += getText();
    }
    ;
NEWLINE_EM
    :    [\r\n]+  {handleError(2, "EMAIL");}
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
    :    '<'
    {
        if (errorString.length() > 0) {
            errorString += getText();
        } else {
            print("DATE", dateString);
            dateString = "";
            popMode();
        }
    }
    ;
DATE_ERROR_CATCHING
    :    ~([\r\n] | '<')
    {
        if (dateString.length() > 0) {
            errorString += dateString;
            dateString = "";
        }
        errorString += getText();
    }
    ;
NEWLINE_DM
    :    [\r\n]+  {handleError(2, "DATE");}
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
    :    '<'
    {
        if (errorString.length() > 0) {
            errorString += getText();
        } else {
            print("PHONE", phoneString);
            phoneString = "";
            popMode();
        }
    }
    ;
PHONE_ERROR_CATCHING
    :    ~([\r\n] | '<')
    {
        if (phoneString.length() > 0) {
            errorString += phoneString;
            phoneString = "";
        }
        errorString += getText();
    }
    ;
NEWLINE_PM
    :    [\r\n]+  {handleError(2, "PHONE");}
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
    :    '<'
    {
        if (errorString.length() > 0) {
            errorString += getText();
        } else {
            print("CREDITCARD", cardString);
            cardString = "";
            popMode();
        }
    }
    ;
CARD_ERROR_CATCHING
    :    ~([\r\n] | '<')
    {
        if (cardString.length() > 0) {
            errorString += cardString;
            cardString = "";
        }
        errorString += getText();
    }
    ;
NEWLINE_CM
    :    [\r\n]+  {handleError(2, "CREDITCARD");}
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
    :    '<'
    {
        if (errorString.length() > 0) {
            errorString += getText();
        } else {
            print(lastTag, otherString);
            otherString = "";
            popMode();
        }
    }
    ;
OTHER_ERROR_CATCHING
    :    ~([\r\n] | '<')
    {
        if (otherString.length() > 0) {
            errorString += otherString;
            otherString = "";
        }
        errorString += getText();
    }
    ;
NEWLINE_OM
    :    [\r\n]+  {handleError(2, lastTag);}
    ;
