<#import "template.ftl" as layout>
<@layout.emailLayout>
<h2 style="margin: 0 0 16px; font-size: 22px; font-weight: 700; color: #1A1A2E; line-height: 1.3;">Test Email</h2>

<p style="margin: 0 0 16px; color: #374151;">This is a test message from EasyChamp.</p>

<table role="presentation" cellpadding="0" cellspacing="0" width="100%" style="margin: 0 0 16px;">
    <tr>
        <td style="background-color: #F0FDF4; border-radius: 8px; padding: 16px 20px;">
            <p style="margin: 0; font-size: 14px; color: #166534;">If you received this, your email configuration is working correctly.</p>
        </td>
    </tr>
</table>
</@layout.emailLayout>
