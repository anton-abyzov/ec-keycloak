<#import "template.ftl" as layout>
<@layout.emailLayout>
<h2 style="margin: 0 0 8px; font-size: 22px; font-weight: 700; color: #1A1A2E; line-height: 1.3;">Update Your Account</h2>
<p style="margin: 0 0 24px; font-size: 13px; color: #9CA3AF;">For account: <strong style="color: #6C63FF;">${user.email}</strong></p>

<p style="margin: 0 0 16px; color: #374151;">Your administrator has requested that you update your EasyChamp account by performing the following action(s):</p>

<table role="presentation" cellpadding="0" cellspacing="0" width="100%" style="margin: 0 0 20px;">
    <tr>
        <td style="background-color: #F5F3FF; border-radius: 8px; padding: 16px 20px;">
            <p style="margin: 0; font-weight: 600; color: #6C63FF; font-size: 14px;">
                <#if requiredActions??>
                    <#list requiredActions as reqActionItem>${msg("requiredAction.${reqActionItem}")}<#sep>, </#list>
                </#if>
            </p>
        </td>
    </tr>
</table>

<table role="presentation" cellpadding="0" cellspacing="0" width="100%" style="margin: 8px 0 24px;">
    <tr>
        <td align="center">
            <a href="${link}" target="_blank" style="display: inline-block; background: linear-gradient(135deg, #6C63FF, #7C74FF); color: #ffffff; text-decoration: none; padding: 14px 40px; border-radius: 10px; font-weight: 600; font-size: 15px; letter-spacing: 0.2px;">Update Account</a>
        </td>
    </tr>
</table>

<p style="margin: 0 0 20px; font-size: 13px; color: #9CA3AF; line-height: 1.6;">This link will expire in <strong style="color: #6B7280;">${linkExpirationFormatter(linkExpiration)}</strong>.</p>

<table role="presentation" cellpadding="0" cellspacing="0" width="100%">
    <tr>
        <td style="border-top: 1px solid #F3F4F6; padding-top: 20px;">
            <p style="margin: 0; font-size: 13px; color: #9CA3AF; line-height: 1.6;">If you are unaware of this request, just ignore this email and nothing will be changed.</p>
        </td>
    </tr>
</table>
</@layout.emailLayout>
