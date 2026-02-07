<#import "template.ftl" as layout>
<@layout.emailLayout>
<h2 style="margin: 0 0 8px; font-size: 22px; font-weight: 700; color: #1A1A2E; line-height: 1.3;">Reset Your Password</h2>
<p style="margin: 0 0 24px; font-size: 13px; color: #9CA3AF;">For account: <strong style="color: #6C63FF;">${user.email}</strong></p>

<p style="margin: 0 0 16px; color: #374151;">We received a request to reset the password for your EasyChamp account. Click the button below to choose a new password:</p>

<table role="presentation" cellpadding="0" cellspacing="0" width="100%" style="margin: 8px 0 24px;">
    <tr>
        <td align="center">
            <a href="${link}" target="_blank" style="display: inline-block; background: linear-gradient(135deg, #6C63FF, #7C74FF); color: #ffffff; text-decoration: none; padding: 14px 40px; border-radius: 10px; font-weight: 600; font-size: 15px; letter-spacing: 0.2px;">Reset Password</a>
        </td>
    </tr>
</table>

<p style="margin: 0 0 20px; font-size: 13px; color: #9CA3AF; line-height: 1.6;">This link will expire in <strong style="color: #6B7280;">${linkExpirationFormatter(linkExpiration)}</strong>.</p>

<table role="presentation" cellpadding="0" cellspacing="0" width="100%">
    <tr>
        <td style="border-top: 1px solid #F3F4F6; padding-top: 20px;">
            <p style="margin: 0; font-size: 13px; color: #9CA3AF; line-height: 1.6;">If you didn't request a password reset, you can safely ignore this email. Your password will not be changed.</p>
        </td>
    </tr>
</table>
</@layout.emailLayout>
