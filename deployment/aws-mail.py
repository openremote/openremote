#!/usr/bin/python3

import smtplib
import os

sender    = 'no-reply@openremote.io'
receivers = [os.environ['WHO']]
subject   = 'Gitlab pipeline FAILED: '+os.environ['WHAT']

print(sender,receivers, subject)
message = '\r\n'.join(['From: %s' % sender,
                       'To: %s' % receivers[0],
                       'Subject: %s' % subject,
                      """
The GitLab CI/CD pipeline FAILED for {repo}

{what} 
{when} 
{who}.
""".format(when=os.environ['WHEN'], who=os.environ['WHO'], what=os.environ['WHAT'], repo=os.environ['REPO'])
                      ])

try:
   server = smtplib.SMTP('email-smtp.eu-west-1.amazonaws.com',587)
   server.ehlo()
   server.starttls()
   server.login(os.environ['SMTP_USERNAME'], os.environ['SMTP_PASSWORD'])

   server.sendmail(sender, receivers, message)         
   print ("Successfully sent email")
except Exception as ex:
   print ("Error: unable to send email for "+os.environ['SMTP_USERNAME'])

   template = "An exception of type {0} occurred. Arguments:\n{1!r}"
   print (template.format(type(ex).__name__, ex.args))
