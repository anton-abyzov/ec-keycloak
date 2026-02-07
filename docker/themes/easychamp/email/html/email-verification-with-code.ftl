<#import "template.ftl" as layout>
<@layout.emailLayout>
<h2 style="margin: 0 0 8px; font-size: 22px; font-weight: 700; color: #1A1A2E; line-height: 1.3;">Verify Your Email</h2>
<p style="margin: 0 0 24px; font-size: 13px; color: #9CA3AF;">For account: <strong style="color: #6C63FF;">${user.email}</strong></p>

<p style="margin: 0 0 20px; color: #374151;">Please verify your email address by entering the following code:</p>

<table role="presentation" cellpadding="0" cellspacing="0" width="100%" style="margin: 0 0 24px;">
    <tr>
        <td align="center">
            <span style="display: inline-block; background-color: #F5F3FF; border: 2px solid #6C63FF; border-radius: 12px; padding: 16px 40px; font-size: 32px; font-weight: 700; letter-spacing: 6px; color: #1A1A2E; font-family: 'Courier New', monospace;">${code}</span>
        </td>
    </tr>
</table>

<p style="margin: 0 0 20px; font-size: 13px; color: #9CA3AF; line-height: 1.6;">This code will expire in <strong style="color: #6B7280;">${linkExpirationFormatter(linkExpiration)}</strong>.</p>

<table role="presentation" cellpadding="0" cellspacing="0" width="100%">
    <tr>
        <td style="border-top: 1px solid #F3F4F6; padding-top: 20px;">
            <p style="margin: 0; font-size: 13px; color: #9CA3AF; line-height: 1.6;">If you didn't request this code, you can safely ignore this email.</p>
        </td>
    </tr>
</table>
</@layout.emailLayout>
