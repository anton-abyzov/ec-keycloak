<#macro emailLayout>
<!DOCTYPE html>
<html lang="en" dir="ltr">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <title>EasyChamp</title>
    <!--[if mso]>
    <noscript>
        <xml>
            <o:OfficeDocumentSettings>
                <o:PixelsPerInch>96</o:PixelsPerInch>
            </o:OfficeDocumentSettings>
        </xml>
    </noscript>
    <![endif]-->
</head>
<body style="margin: 0; padding: 0; background-color: #EEEDF5; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; -webkit-font-smoothing: antialiased; -moz-osx-font-smoothing: grayscale;">
    <div style="display: none; max-height: 0; overflow: hidden; mso-hide: all;">
        &nbsp;&zwnj;&nbsp;&zwnj;&nbsp;&zwnj;&nbsp;&zwnj;&nbsp;&zwnj;
    </div>

    <table role="presentation" cellpadding="0" cellspacing="0" width="100%" style="background-color: #EEEDF5;">
        <tr>
            <td align="center" style="padding: 48px 20px 32px;">

                <!-- Logo -->
                <table role="presentation" cellpadding="0" cellspacing="0" width="560" style="max-width: 560px; width: 100%;">
                    <tr>
                        <td align="center" style="padding-bottom: 32px;">
                            <span style="font-family: 'Georgia', 'Times New Roman', serif; font-size: 26px; font-weight: 700; color: #1A1A2E; letter-spacing: -0.5px;">EasyChamp</span>
                        </td>
                    </tr>
                </table>

                <!-- Card -->
                <table role="presentation" cellpadding="0" cellspacing="0" width="560" style="max-width: 560px; width: 100%;">
                    <tr>
                        <td style="background-color: #ffffff; border-radius: 16px; overflow: hidden;">
                            <!-- Top accent -->
                            <table role="presentation" cellpadding="0" cellspacing="0" width="100%">
                                <tr>
                                    <td style="height: 4px; background: linear-gradient(135deg, #6C63FF 0%, #8B7FFF 50%, #A78BFA 100%); font-size: 0; line-height: 0;">&nbsp;</td>
                                </tr>
                            </table>
                            <!-- Content -->
                            <table role="presentation" cellpadding="0" cellspacing="0" width="100%">
                                <tr>
                                    <td style="padding: 44px 48px 40px; color: #374151; font-size: 15px; line-height: 1.7;">
                                        <#nested>
                                    </td>
                                </tr>
                            </table>
                        </td>
                    </tr>
                </table>

                <!-- Footer -->
                <table role="presentation" cellpadding="0" cellspacing="0" width="560" style="max-width: 560px; width: 100%;">
                    <tr>
                        <td align="center" style="padding: 32px 20px 0;">
                            <p style="margin: 0 0 6px; font-size: 13px; color: #9CA3AF; line-height: 1.5;">
                                Sports competition management platform
                            </p>
                            <p style="margin: 0; font-size: 12px; color: #D1D5DB;">
                                &copy; ${.now?string("yyyy")} EasyChamp. All rights reserved.
                            </p>
                        </td>
                    </tr>
                </table>

            </td>
        </tr>
    </table>
</body>
</html>
</#macro>
