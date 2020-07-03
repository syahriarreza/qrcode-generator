function typeFilter(element) {
    element.kendoDropDownList({
        dataSource: ["File", "Folder"],
        optionLabel: "--Select Value--"
    });
}

$(document).ready(function () {
    $("#pager").kendoGrid({
        dataSource: {
            pageSize: 20
        },
        height: 550,
        sortable: true,
        scrollable: true,
        sortable: true,
        filterable: {
            extra: false,
        },
        pageable: {
            refresh: false,
            pageSizes: true,
            buttonCount: 5
        },
        columns: [
            {
                field: "Name",
                filterable: {
                    operators: {
                        string: {
                            contains: "Contains",
                        },
                    },
                },
            },
            {
                field: "Type",
                width: "18%",
                filterable: {
                    ui: typeFilter,
                    operators: {
                        string: {
                            eq: "Equal",
                        },
                    },
                }
            },
        ],
    });
});